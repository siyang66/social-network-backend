package com.meet5.social.config;

public class DataSourceContext {

    private static final ThreadLocal<DataSourceType> holder = new ThreadLocal<>();

    public static void setMaster() {
        holder.set(DataSourceType.MASTER);
    }

    public static void setSlave() {
        holder.set(DataSourceType.SLAVE);
    }

    public static DataSourceType get() {
        return holder.get();
    }

    public static void clear() {
        holder.remove();
    }
}
