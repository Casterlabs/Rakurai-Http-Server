package co.casterlabs.rakurai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@NonNull
@AllArgsConstructor
public enum DataSize {
    BYTE(1, "B"),

    KILOBYTE(DataSize.K_SCALE, "KB"),
    KIBIBYTE(DataSize.KI_SCALE, "KiB"),

    MEGABYTE(DataSize.M_SCALE, "M"),
    MEBIBYTE(DataSize.MI_SCALE, "MiB"),

    GIGABYTE(DataSize.G_SCALE, "GB"),
    GIBIBYTE(DataSize.GI_SCALE, "GiB"),

    TERABYTE(DataSize.T_SCALE, "TB"),
    TEBIBYTE(DataSize.TI_SCALE, "TiB"),

    PETABYTE(DataSize.P_SCALE, "PB"),
    PEBIBYTE(DataSize.PI_SCALE, "PiB");

    private static final double K_SCALE = 1000;
    private static final double KI_SCALE = 1024;

    private static final double M_SCALE = K_SCALE * 1000;
    private static final double MI_SCALE = KI_SCALE * 1024;

    private static final double G_SCALE = M_SCALE * 1000;
    private static final double GI_SCALE = MI_SCALE * 1024;

    private static final double T_SCALE = G_SCALE * 1000;
    private static final double TI_SCALE = GI_SCALE * 1024;

    private static final double P_SCALE = T_SCALE * 1000;
    private static final double PI_SCALE = TI_SCALE * 1024;

    private double multiplier;
    private @Getter String suffix;

    public String format(double amount) {
        return String.format("%.2f%s", amount, this.suffix);
    }

    public String format(int amount) {
        return amount + this.suffix;
    }

    public static String friendlyFormat(double amount) {
        DataSize size = DataSize.BYTE;

        for (DataSize possible : values()) {
            if (amount >= possible.multiplier) {
                size = possible;
            }
        }

        return size.format(size.fromBytes(amount));
    }

    public static String friendlyFormat(int amount) {
        DataSize size = DataSize.BYTE;

        for (DataSize possible : values()) {
            if (amount >= possible.multiplier) {
                size = possible;
            }
        }

        return size.format((int) size.fromBytes(amount));
    }

    /* Conversions */
    public double fromBytes(double amount) {
        return amount / this.multiplier;
    }

    public double toBytes(double amount) {
        return this.multiplier * amount;
    }

    public double toKiloBytes(double amount) {
        double bytes = this.multiplier * amount;

        return bytes / K_SCALE;
    }

    public double toKibiBytes(double amount) {
        double bytes = this.multiplier * amount;

        return bytes / KI_SCALE;
    }

    public double toMegaBytes(double amount) {
        double bytes = this.multiplier * amount;

        return bytes / M_SCALE;
    }

    public double toMebiBytes(double amount) {
        double bytes = this.multiplier * amount;

        return bytes / MI_SCALE;
    }

    public double toGigaBytes(double amount) {
        double bytes = this.multiplier * amount;

        return bytes / G_SCALE;
    }

    public double toGibiBytes(double amount) {
        double bytes = this.multiplier * amount;

        return bytes / GI_SCALE;
    }

    public double toTeraBytes(double amount) {
        double bytes = this.multiplier * amount;

        return bytes / T_SCALE;
    }

    public double toTebiBytes(double amount) {
        double bytes = this.multiplier * amount;

        return bytes / TI_SCALE;
    }

    public double toPetaBytes(double amount) {
        double bytes = this.multiplier * amount;

        return bytes / P_SCALE;
    }

    public double toPebiBytes(double amount) {
        double bytes = this.multiplier * amount;

        return bytes / PI_SCALE;
    }

}
