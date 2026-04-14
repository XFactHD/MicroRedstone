package io.github.xfacthd.microredstone.client.screen.widgets;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TriState;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

public final class NumberEditBox extends ValidatingEditBox {
    private final ParserConfig config;

    public NumberEditBox(Font font, int x, int y, int width, int height, ParserConfig config, int defaultValue) {
        this(font, x, y, width, height, config, null, defaultValue);
    }

    public NumberEditBox(Font font, int x, int y, int width, int height, ParserConfig config, @Nullable NumberEditBox prevEditBox, int defaultValue) {
        super(font, x, y, width, height, toValidator(config), prevEditBox, formatValue(config, defaultValue));
        this.config = config;
        setTextAlignment(TextAlignment.RIGHT);
    }

    private static String formatValue(ParserConfig config, int defaultValue) {
        NumberFormat format = NumberFormat.getPreferred(config.validFormats.keySet());
        return format.formatter.apply(defaultValue);
    }

    public int getIntValue() {
        try {
            String value = getValue();
            NumberFormat format = NumberFormat.ofPrefix(value);
            return format.parser.applyAsInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setIntValue(int value) {
        setValue(formatValue(config, value));
    }

    private static ValidatingEditBox.Validator toValidator(ParserConfig config) {
        return new ValidatingEditBox.Validator() {
            @Override
            public TriState validate(String value) {
                if (value.isEmpty()) {
                    return TriState.DEFAULT;
                }

                NumberFormat format = NumberFormat.ofPrefix(value);
                if (!config.isValidFormat(format)) {
                    if (NumberFormat.isPrefixStart(value)) {
                        return TriState.DEFAULT;
                    }
                    return TriState.FALSE;
                }

                String matchValue = value.substring(format.prefix.length());
                if (matchValue.isEmpty()) {
                    return TriState.DEFAULT;
                }
                if (!format.pattern.matcher(matchValue).matches()) {
                    return TriState.FALSE;
                }
                if (config.getMaxLength(format) < matchValue.length()) {
                    return TriState.FALSE;
                }

                if (value.length() == 1 && value.charAt(0) == '-' && (!config.supportNegative || format != NumberFormat.DEC)) {
                    return TriState.FALSE;
                }
                try {
                    boolean result = config.validator.test(format.parser.applyAsInt(value));
                    return result ? TriState.TRUE : TriState.DEFAULT;
                } catch (NumberFormatException e) {
                    return TriState.FALSE;
                }
            }

            @Override
            public @Nullable Component getInvalidValueTooltip() {
                return config.invalidValueTooltip;
            }
        };
    }

    public record ParserConfig(
            IntPredicate validator,
            Reference2IntMap<NumberFormat> validFormats,
            boolean supportNegative,
            @Nullable Component invalidValueTooltip
    ) {
        public ParserConfig(
                IntPredicate validator,
                Set<NumberFormat> validFormats,
                boolean supportNegative,
                @Nullable Component invalidValueTooltip
        ) {
            this(validator, makeFormatMap(validFormats), supportNegative, invalidValueTooltip);
        }

        public ParserConfig {
            Preconditions.checkArgument(!validFormats.isEmpty(), "At least one format must be specified");
        }

        boolean isValidFormat(NumberFormat format) {
            return validFormats.containsKey(format);
        }

        int getMaxLength(NumberFormat format) {
            return validFormats.getInt(format);
        }

        private static Reference2IntMap<NumberFormat> makeFormatMap(Set<NumberFormat> validFormats) {
            Reference2IntMap<NumberFormat> map = new Reference2IntOpenHashMap<>(validFormats.size());
            for (NumberFormat format : validFormats) {
                map.put(format, Integer.MAX_VALUE);
            }
            return map;
        }
    }

    public enum NumberFormat {
        DEC("", Pattern.compile("(-?[0-9]*)"), Integer::toString, Integer::parseInt),
        HEX("0x", Pattern.compile("([0-9a-fA-F]*)"), Integer::toHexString, value -> Integer.parseInt(value, 16)),
        BIN("0b", Pattern.compile("([01]*)"), Integer::toBinaryString, value -> Integer.parseInt(value, 2)),
        ;

        private static final NumberFormat[] FORMATS = values();

        private final String prefix;
        private final Pattern pattern;
        private final IntFunction<String> formatter;
        private final ToIntFunction<String> parser;

        NumberFormat(String prefix, Pattern pattern, IntFunction<String> formatter, ToIntFunction<String> parser) {
            this.prefix = prefix;
            this.pattern = pattern;
            this.formatter = prefix.isEmpty() ? formatter : value -> prefix + formatter.apply(value);
            this.parser = prefix.isEmpty() ? parser : value -> parser.applyAsInt(value.substring(prefix.length()));
        }

        static boolean isPrefixStart(String text) {
            for (int i = 1; i < FORMATS.length; i++) {
                String prefix = FORMATS[i].prefix;
                if (prefix.length() > text.length() && prefix.startsWith(text)) {
                    return true;
                }
            }
            return false;
        }

        static NumberFormat ofPrefix(String text) {
            for (int i = 1; i < FORMATS.length; i++) {
                NumberFormat format = FORMATS[i];
                if (text.startsWith(format.prefix)) {
                    return format;
                }
            }
            return DEC;
        }

        static NumberFormat getPreferred(Set<NumberFormat> validFormats) {
            for (NumberFormat format : FORMATS) {
                if (validFormats.contains(format)) {
                    return format;
                }
            }
            throw new IllegalStateException("No preferred format found");
        }
    }
}
