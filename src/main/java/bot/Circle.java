package bot;

import java.util.function.Function;

public class Circle {
    public double height(double area, double baseHeight) {
        var piArea = area * Math.PI + heightToArea(baseHeight);
        var hMin = 0;
        var hMax = 1;
        var hAvg = (hMin + hMax) / 2;
        var hAvgArea = heightToArea(hAvg);
        while (Math.abs(piArea - hAvgArea) > 0.01) {
            if (hAvgArea > piArea) {
                hMax = hAvg;
            } else {
                hMin = hAvg;
            }
            hAvg = (hMin + hMax) / 2;
            heightToArea(hAvg);
        }
        return hAvg;
    }

    public double heightToArea(double baseHeight) {
        return baseHeight > 0.5 ? Math.PI - heightToArea(1 - baseHeight) : sectionArea(Math.acos(0.5 - baseHeight) * 2);
    }

    public double sectionArea(double alpha) {
        return 0.5 * (alpha - Math.sin(alpha));
    }

    public double width(double area, double startHeight, double endHeight, double share) {
        if (endHeight - startHeight > 0.5) throw new UnsupportedOperationException("High heights not supported");
        if (share > 0.5) return 1 - width(area, startHeight, endHeight, 1 - share);
        if (endHeight > 0.999 && startHeight < 0.001) {
            return share;
        } else if (endHeight > 0.999) {
            return width(area, 0, 1 - startHeight, share);
        } else if (startHeight < 0.001) {
            return binarySearch(area * share, d -> outerTwoDimCutSection(d, endHeight));
        } else {
            return binarySearch(area * share, d -> outerTwoDimCutSection(d, endHeight));
        }
    }

    private double binarySearch(double target, Function<Double, Double> compute) {
        var wMin = 0.0;
        var wMax = 1.0;
        var wAvg = (wMin + wMax) / 2;
        var wAvgArea = compute.apply(wAvg);
        while (Math.abs(target - wAvgArea) > 0.01) {
            if (wAvgArea > target) {
                wMax = wAvg;
            } else {
                wMin = wAvg;
            }
            wAvg = (wMin + wMax) / 2;
            wAvgArea = compute.apply(wAvg);
        }
        return wAvg;
    }

    public double outerTwoDimCutSection(double x, double y) {
        return 0;
    }
}
