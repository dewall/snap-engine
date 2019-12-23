package org.esa.snap.core.image;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.*;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.util.ArrayList;

/**
 * Created by jcoravu on 11/12/2019.
 */
public abstract class AbstractMosaicSubsetMultiLevelSource<TileDataType> extends AbstractMultiLevelSource {

    protected final Rectangle visibleImageBounds;

    private final Dimension tileSize;
    private final TileImageDisposer tileImageDisposer;

    protected AbstractMosaicSubsetMultiLevelSource(Rectangle visibleImageBounds, Dimension tileSize, GeoCoding geoCoding) {
        this(DefaultMultiLevelModel.getLevelCount(visibleImageBounds.width, visibleImageBounds.height), visibleImageBounds, tileSize, geoCoding);
    }

    protected AbstractMosaicSubsetMultiLevelSource(int levelCount, Rectangle visibleImageBounds, Dimension tileSize, GeoCoding geoCoding) {
        super(new DefaultMultiLevelModel(levelCount, Product.findImageToModelTransform(geoCoding), visibleImageBounds.width, visibleImageBounds.height));

        if (visibleImageBounds.width < tileSize.width) {
            throw new IllegalArgumentException("The visible image width " + visibleImageBounds.width + " cannot be smaller than the tile width " + tileSize.width + ".");
        }
        if (visibleImageBounds.height < tileSize.height) {
            throw new IllegalArgumentException("The visible image height " + visibleImageBounds.height + " cannot be smaller than the tile height " + tileSize.height + ".");
        }

        this.visibleImageBounds = visibleImageBounds;
        this.tileSize = tileSize;

        this.tileImageDisposer = new TileImageDisposer();
    }

    protected abstract SourcelessOpImage buildTileOpImage(Rectangle visibleBounds, int level, Point tileOffset, Dimension tileSize, TileDataType tileData);

    @Override
    public synchronized void reset() {
        super.reset();

        this.tileImageDisposer.disposeAll();
        System.gc();
    }

    protected final java.util.List<RenderedImage> buildTileImages(int level, Rectangle visibleBounds, float translateLevelOffsetX, float translateLevelOffsetY, TileDataType tileData) {
        int columnTileCount = ImageUtils.computeTileCount(visibleBounds.width, this.tileSize.width);
        float imageLevelWidth;
        if (columnTileCount > 1) {
            // for more than one tile column compute the image width for the specified level as a float number
            imageLevelWidth = (float) ImageUtils.computeLevelSizeAsDouble(visibleBounds.width, level);
        } else if (columnTileCount == 1) {
            // for only one tile column compute the image width for the specified level as an integer number
            imageLevelWidth = ImageUtils.computeLevelSize(visibleBounds.width, level);
        } else {
            throw new IllegalArgumentException("Invalid column tile count: " + columnTileCount);
        }

        int rowTileCount = ImageUtils.computeTileCount(visibleBounds.height, this.tileSize.height);
        float imageLevelHeight;
        if (rowTileCount > 1) {
            // for more than one tile row compute the image height for the specified level as a float number
            imageLevelHeight = (float) ImageUtils.computeLevelSizeAsDouble(visibleBounds.height, level);
        } else if (rowTileCount == 1) {
            // for only one tile row compute the image height for the specified level as an integer number
            imageLevelHeight = ImageUtils.computeLevelSize(visibleBounds.height, level);
        } else {
            throw new IllegalArgumentException("Invalid row tile count: " + rowTileCount);
        }

        java.util.List<RenderedImage> tileImages = new ArrayList<>(columnTileCount * rowTileCount);
        float xTranslateWidth = (float) ImageUtils.computeLevelSizeAsDouble(this.tileSize.width, level);
        float yTranslateHeight = (float) ImageUtils.computeLevelSizeAsDouble(this.tileSize.height, level);
        float totalTranslateLevelWidth = 0.0f;
        float totalTranslateLevelHeight = 0.0f;
        for (int tileRowIndex = 0; tileRowIndex < rowTileCount; tileRowIndex++) {
            int tileOffsetY = tileRowIndex * this.tileSize.height;
            boolean isLastRow = (tileRowIndex == rowTileCount - 1);
            int tileHeight = isLastRow ? (visibleBounds.height - tileOffsetY) : this.tileSize.height;
            for (int tileColumnIndex = 0; tileColumnIndex < columnTileCount; tileColumnIndex++) {
                int tileOffsetX = tileColumnIndex * this.tileSize.width;
                boolean isLastColumn = (tileColumnIndex == columnTileCount - 1);
                int tileWidth = isLastColumn ? (visibleBounds.width - tileOffsetX) : this.tileSize.width;

                Dimension currentTileSize = new Dimension(tileWidth, tileHeight);
                Point tileOffset = new Point(tileOffsetX, tileOffsetY);
                SourcelessOpImage tileOpImage = buildTileOpImage(visibleBounds, level, tileOffset, currentTileSize, tileData);
                this.tileImageDisposer.registerForDisposal(tileOpImage);

                float translateX = computeTranslateOffset(tileColumnIndex, columnTileCount, xTranslateWidth, tileOpImage.getWidth(), imageLevelWidth);
                if (translateX < 0.0f) {
                    throw new IllegalStateException("The translate x is negative: "+ translateX);
                }
                float translateY = computeTranslateOffset(tileRowIndex, rowTileCount, yTranslateHeight, tileOpImage.getHeight(), imageLevelHeight);
                if (translateY < 0.0f) {
                    throw new IllegalStateException("The translate y is negative: "+ translateY);
                }
                RenderedOp opImage = TranslateDescriptor.create(tileOpImage, translateLevelOffsetX + translateX, translateLevelOffsetY + translateY, Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);
                tileImages.add(opImage);
                if (isLastRow && isLastColumn) {
                    totalTranslateLevelWidth = translateX + tileOpImage.getWidth();
                    totalTranslateLevelHeight = translateY + tileOpImage.getHeight();
                }
            }
        }
        if (imageLevelWidth != totalTranslateLevelWidth) {
            throw new IllegalStateException("Invalid width: imageLevelWidth="+imageLevelWidth+", totalTranslateWidth="+totalTranslateLevelWidth);
        }
        if (imageLevelHeight != totalTranslateLevelHeight) {
            throw new IllegalStateException("Invalid height: imageLevelHeight="+imageLevelHeight+", totalTranslateHeight="+totalTranslateLevelHeight);
        }

        return tileImages;
    }

    protected final RenderedOp buildMosaicOp(int level, java.util.List<RenderedImage> tileImages) {
        if (tileImages.size() == 0) {
            throw new IllegalStateException("No tiles found.");
        }
        int imageLevelWidth = ImageUtils.computeLevelSize(this.visibleImageBounds.width, level);
        int imageLevelHeight = ImageUtils.computeLevelSize(this.visibleImageBounds.height, level);

        Dimension defaultTileSize = JAI.getDefaultTileSize();
        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setMinX(0);
        imageLayout.setMinY(0);
        imageLayout.setTileWidth(defaultTileSize.width); // set the default JAI tile width
        imageLayout.setTileHeight(defaultTileSize.height); // set the default JAI tile height
        imageLayout.setTileGridXOffset(0);
        imageLayout.setTileGridYOffset(0);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        RenderedImage[] sources = tileImages.toArray(new RenderedImage[tileImages.size()]);

        RenderedOp mosaicOp = MosaicDescriptor.create(sources, MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null, null, hints);

        if (mosaicOp.getWidth() > imageLevelWidth) {
            throw new IllegalStateException("The mosaic operator width " + mosaicOp.getWidth() + " > than the image width " + imageLevelWidth + ".");
        }
        if (mosaicOp.getHeight() > imageLevelHeight) {
            throw new IllegalStateException("The mosaic operator height " + mosaicOp.getWidth() + " > than the image height " + imageLevelHeight + ".");
        }
        if (mosaicOp.getWidth() < imageLevelWidth || mosaicOp.getHeight() < imageLevelHeight) {
            int rightPad = imageLevelWidth - mosaicOp.getWidth();
            int bottomPad = imageLevelHeight - mosaicOp.getHeight();
            BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
            mosaicOp = BorderDescriptor.create(mosaicOp, 0, rightPad, 0, bottomPad, borderExtender, null);
        }

        return mosaicOp;
    }

    private static float computeTranslateOffset(int tileIndex, int tileCount, float translateSize, int imageSize, float imageLevelTotalSize) {
        float translateOffset = tileIndex * translateSize;
        if (translateOffset + imageSize > imageLevelTotalSize) {
            if (tileIndex == tileCount - 1) {
                if (imageLevelTotalSize < imageSize) {
                    throw new IllegalStateException("Invalid values: imageLevelTotalSize="+imageLevelTotalSize+", imageSize="+imageSize);
                }
                translateOffset = imageLevelTotalSize - imageSize; // the last row
            } else {
                throw new IllegalStateException("Invalid values: translateSize="+translateSize+", translateOffset="+translateOffset+", imageSize="+imageSize+", imageLevelTotalSize="+imageLevelTotalSize);
            }
        }
        return translateOffset;
    }
}