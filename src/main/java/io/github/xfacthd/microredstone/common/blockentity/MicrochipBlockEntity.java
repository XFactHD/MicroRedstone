package io.github.xfacthd.microredstone.common.blockentity;

import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.Circuit;
import io.github.xfacthd.microredstone.common.circuit.ExternalInterfaceAdapter;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.data.PropertyHolder;
import io.github.xfacthd.microredstone.common.data.component.StoredCircuit;
import io.github.xfacthd.microredstone.common.menu.MicrochipMenu;
import io.github.xfacthd.microredstone.common.redstone.BundledWireSupport;
import io.github.xfacthd.microredstone.common.redstone.RedstoneLevelAdapter;
import io.github.xfacthd.microredstone.common.redstone.RedstoneType;
import io.github.xfacthd.microredstone.common.util.SerdesUtils;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

public final class MicrochipBlockEntity extends BaseBlockEntity implements RedstoneLevelAdapter, ExternalInterfaceAdapter, MenuProvider
{
    public static final Component MENU_TITLE = Utils.translate("title", "microchip");
    public static final ModelProperty<RedstoneType[]> PORT_TYPE_PROPERTY = new ModelProperty<>();
    private static final Port[] PORTS = Port.values();
    private static final Rotation[] ROTATIONS = Rotation.values();

    private final RedstoneType[] portTypes = Utils.fillArray(new RedstoneType[4], $ -> RedstoneType.NONE);
    private final short[] portStates = new short[4];
    private Direction facing = Direction.DOWN;
    private Rotation rotation = Rotation.NONE;
    @Nullable
    private Circuit circuit = null;
    private String circuitName = "";

    public MicrochipBlockEntity(BlockPos pos, BlockState state)
    {
        super(MRContent.BLOCK_ENTITY_MICROCHIP.value(), pos, state);
        setBlockState(state);
    }

    public void tick()
    {
        if (circuit != null)
        {
            circuit.evaluate(this);
        }
    }

    public void setCircuit(@Nullable StoredCircuit circuit)
    {
        if (circuit != null && circuit.rootNode() != null)
        {
            setCircuit(circuit.name(), circuit.toCircuit());
        }
        else
        {
            setCircuit("", null);
        }
    }

    public void setCircuit(String circuitName, @Nullable Circuit circuit)
    {
        boolean[] signalUpdates = new boolean[4];
        boolean hadCircuit = this.circuit != null;
        if (hadCircuit)
        {
            Utils.fillArray(portTypes, $ -> RedstoneType.NONE);
            for (Connector output : this.circuit.getOutputs())
            {
                int portIdx = output.port().ordinal();
                if (portStates[portIdx] != 0)
                {
                    portStates[portIdx] = 0;
                    signalUpdates[portIdx] = true;
                }
            }
        }
        this.circuit = circuit;
        this.circuitName = circuitName;
        boolean hasCircuit = circuit != null;
        if (hasCircuit)
        {
            for (Connector input : circuit.getInputs())
            {
                portTypes[input.port().ordinal()] = RedstoneType.of(input.type());
            }
            for (Connector output : circuit.getOutputs())
            {
                portTypes[output.port().ordinal()] = RedstoneType.of(output.type());
            }
        }

        if (hadCircuit != hasCircuit)
        {
            BlockState state = getBlockState().setValue(PropertyHolder.HAS_CIRCUIT, hasCircuit);
            level().setBlockAndUpdate(worldPosition, state);
        }
        else
        {
            sendClientUpdate();
        }

        setChangedWithoutSignalUpdate();
        for (Port port : PORTS)
        {
            int portIdx = port.ordinal();
            if (signalUpdates[portIdx])
            {
                triggerSignalUpdate(portIdx);
            }
        }
    }

    @Nullable
    public Circuit getCircuit()
    {
        return circuit;
    }

    public String getCircuitName()
    {
        return circuitName;
    }

    @Override
    public short read(int input)
    {
        return portStates[input];
    }

    @Override
    public void write(int output, short value)
    {
        if (value != portStates[output])
        {
            portStates[output] = value;
            setChangedWithoutSignalUpdate();
            triggerSignalUpdate(output);
        }
    }

    @Override
    public RedstoneType getRedstoneType(Direction side)
    {
        return portTypes[getSideRotation(facing, side).ordinal()];
    }

    @Override
    public int getRedstoneOutput(Direction side)
    {
        Rotation sideRot = getSideRotation(facing, side);
        return switch (portTypes[sideRot.ordinal()])
        {
            case NONE -> 0;
            case SINGLE -> portStates[sideRot.ordinal()] * 15;
            case BUNDLED -> portStates[sideRot.ordinal()];
        };
    }

    @Override
    public void handleNeighborUpdate(BlockPos adjPos, Direction side)
    {
        Rotation sideRot = getSideRotation(facing, side);
        RedstoneType portType = portTypes[sideRot.ordinal()];
        portStates[sideRot.ordinal()] = readExternalInput(adjPos, side, portType);
    }

    private Rotation getSideRotation(Direction facing, Direction side)
    {
        Rotation sideRot = Utils.getRotationFromFacingOrientation(facing, side);
        return sideRot.getRotated(Utils.invertRotation(rotation));
    }

    private short readExternalInput(BlockPos adjPos, Direction side, RedstoneType portType)
    {
        return switch (portType)
        {
            case NONE -> 0;
            case SINGLE -> (short) (level().hasSignal(adjPos, side) ? 1 : 0);
            case BUNDLED -> BundledWireSupport.getBundledInput(level(), worldPosition, adjPos, side);
        };
    }

    private void triggerSignalUpdate(int port)
    {
        Rotation sideRot = rotation.getRotated(ROTATIONS[port]);
        Direction side = Utils.getSideFromFacingRotation(facing, sideRot);
        BlockPos adjPos = worldPosition.relative(side);
        switch (portTypes[port])
        {
            case NONE -> { }
            case SINGLE -> level().neighborChanged(adjPos, getBlockState().getBlock(), null);
            case BUNDLED -> BundledWireSupport.updateNeighbor(level(), worldPosition, adjPos, side);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return MicrochipMenu.createServer(containerId, inventory, this);
    }

    @Override
    public Component getDisplayName()
    {
        return MENU_TITLE;
    }

    @Override
    public ModelData getModelData()
    {
        return ModelData.of(PORT_TYPE_PROPERTY, portTypes.clone());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput valueInput)
    {
        handleUpdateTag(valueInput);
        level().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries)
    {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        SerdesUtils.writeTypedArray(output, "port_types", RedstoneType.CODEC, portTypes);
        return output.buildResult();
    }

    @Override
    public void handleUpdateTag(ValueInput input)
    {
        SerdesUtils.readTypedArray(input, "port_types", RedstoneType.CODEC, portTypes);
        requestModelDataUpdate();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state)
    {
        super.setBlockState(state);
        facing = state.getValue(BlockStateProperties.FACING);
        rotation = state.getValue(PropertyHolder.ROTATION);
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if (!level().isClientSide())
        {
            boolean hasCircuit = circuit != null;
            if (getBlockState().getValue(PropertyHolder.HAS_CIRCUIT) != hasCircuit)
            {
                level().setBlockAndUpdate(worldPosition, getBlockState().setValue(PropertyHolder.HAS_CIRCUIT, hasCircuit));
            }
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components)
    {
        if (circuit != null)
        {
            StoredCircuit storedCircuit = new StoredCircuit(circuitName, circuit.getSerializableRootNode());
            components.set(MRContent.DC_TYPE_CIRCUIT, storedCircuit);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter componentGetter)
    {
        setCircuit(componentGetter.get(MRContent.DC_TYPE_CIRCUIT));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output)
    {
        output.discard("port_types");
        output.discard("port_states");
        output.discard("circuit");
        output.discard("circuit_name");
    }

    @Override
    protected void loadAdditional(ValueInput input)
    {
        super.loadAdditional(input);
        SerdesUtils.readTypedArray(input, "port_types", RedstoneType.CODEC, portTypes);
        SerdesUtils.readShortArray(input, "port_states", portStates);
        circuit = input.read("circuit", Circuit.CODEC).orElse(null);
        circuitName = input.getStringOr("circuit_name", "");
    }

    @Override
    protected void saveAdditional(ValueOutput output)
    {
        super.saveAdditional(output);
        SerdesUtils.writeTypedArray(output, "port_types", RedstoneType.CODEC, portTypes);
        SerdesUtils.writeShortArray(output, "port_states", portStates);
        output.storeNullable("circuit", Circuit.CODEC, circuit);
        output.putString("circuit_name", circuitName);
    }
}
