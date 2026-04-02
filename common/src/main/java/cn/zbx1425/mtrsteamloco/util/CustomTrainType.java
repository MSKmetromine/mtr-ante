package cn.zbx1425.mtrsteamloco.util;

import mtr.data.TrainType;

public class CustomTrainType {
    private static final String BOGIE_POSITION_1 = "ante:bp1=";
    private static final String BOGIE_POSITION_2 = "ante:bp2=";

    private double bogiePosition1;
    private double bogiePosition2;

    public double getBogiePosition1() {
        return this.bogiePosition1;
    }

    public double getBogiePosition2() {
        return this.bogiePosition2;
    }

    public void setBogiePosition1(double bogiePosition1) {
        this.bogiePosition1 = bogiePosition1;
    }

    public void setBogiePosition2(double bogiePosition2) {
        this.bogiePosition2 = bogiePosition2;
    }

    public static CustomTrainType parse(String type) {
        var spacing = TrainType.getSpacing(type);

        var halfLength = spacing / 2.0;

        var parts = type.split("_");

        var customType = new CustomTrainType();

        customType.setBogiePosition1(-halfLength);
        customType.setBogiePosition2(halfLength);

        for (var part : parts) {
            if (part.startsWith(BOGIE_POSITION_1)) {
                var valueString = part.substring(BOGIE_POSITION_1.length());
                var value = Double.parseDouble(valueString);
                customType.setBogiePosition1(value);
            } else if (part.startsWith(BOGIE_POSITION_2)) {
                var valueString = part.substring(BOGIE_POSITION_2.length());
                var value = Double.parseDouble(valueString);
                customType.setBogiePosition2(value);
            }
        }

        return customType;
    }
}
