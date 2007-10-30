/*
 * $Id: RectangleExtender.java,v 1.1 2007/03/27 12:51:05 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.gpf.support;

import java.awt.Rectangle;

/**
 * Extends a rectangle using a horizontal and vertical extension.
 * The rectangle is limited by the given clipping rectangle
 */
public class RectangleExtender {

    private final Rectangle clippingRect;
    private final int widthExtend;
    private final int heightExtend;

    /**
     * Creates a RectangleExtender
     * 
     * @param clippingRect  The clipping Rectangle.
     * @param widthExtend   The horizontal extension
     * @param heightExtend  The vertical extension
     */
    public RectangleExtender(Rectangle clippingRect, int widthExtend, int heightExtend) {
        this.clippingRect = clippingRect;
        this.widthExtend = widthExtend;
        this.heightExtend = heightExtend;
    }

    /**
     * Extends the given rectangle and clips the result to the clipping rectangle.
     * @param rectangle
     * 
     * @return extended rectangle
     */
    public Rectangle extend(Rectangle rectangle) {
        Rectangle copy = new Rectangle(rectangle);
        copy.grow(widthExtend, heightExtend);
        return copy.intersection(clippingRect);
    }
}
