package org.esa.snap.dem.dataio.copernicus90m;

import org.esa.snap.core.dataop.dem.AbstractElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.Resampling;

public class CopernicusElevationModelDescriptor extends AbstractElevationModelDescriptor {

    private static final String NAME = "Copernicus 90m Europe";


    private final int RASTER_SIZE = 1201;
    private final int RASTER_DEGREE_WIDTH = 1;
    public static final int NUM_X_TILES = 360;
    public static final int NUM_Y_TILES = 120;
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public float getNoDataValue() {
        return 0;
    }

    @Override
    public int getRasterWidth() {
        return RASTER_SIZE;
    }

    @Override
    public int getRasterHeight() {
        return RASTER_SIZE;
    }

    @Override
    public int getTileWidthInDegrees() {
        return RASTER_DEGREE_WIDTH;
    }

    @Override
    public int getTileWidth() {
        return RASTER_SIZE;
    }

    @Override
    public int getNumXTiles() {
        return NUM_X_TILES;
    }

    @Override
    public int getNumYTiles() {
        return NUM_Y_TILES;
    }

    @Override
    public ElevationModel createDem(Resampling resampling) {
        return new CopernicusElevationModel(this, resampling);
    }

    @Override
    public boolean canBeDownloaded() {
        return true;
    }
}
