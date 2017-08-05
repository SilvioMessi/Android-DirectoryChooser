package net.rdrei.android.dirchooser;

import android.os.Parcelable;

import auto.parcel.AutoParcel;

@AutoParcel
public abstract class SmbDirectoryChooserConfig implements Parcelable {
    /**
     * @return Builder for a new SmbDirectoryChooserConfig.
     */
    public static Builder builder() {
        return new AutoParcel_SmbDirectoryChooserConfig.Builder();
    }

    abstract String initialDirectory();

    abstract String address();

    abstract String domain();

    abstract String username();

    abstract String password();

    @AutoParcel.Builder
    public abstract static class Builder {
        public abstract Builder initialDirectory(String s);
        public abstract Builder address(String s);
        public abstract Builder domain(String s);
        public abstract Builder username(String s);
        public abstract Builder password(String s);
        public abstract SmbDirectoryChooserConfig build();
    }
}
