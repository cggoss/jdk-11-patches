/*
 * FontMetricsPatch.java fixes an issue with JDK11's FontMetrics, see https://bugs.openjdk.java.net/browse/JDK-8217509.
 * It is heavily based on FontMetrics.java from OpenJDK 8.
 * ( http://hg.openjdk.java.net/jdk8u/jdk8u/jdk/file/43ca3768126e/src/share/classes/java/awt/FontMetrics.java )
 * 
 * FontMetricsPatch.java is copyright CGG 2019.
 * FontMetrics.java is copyright Oracle and/or its affiliates, see original copyright notice below.
 */

/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.cgg.jdk11patches;

import org.apache.commons.lang3.reflect.FieldUtils;
import sun.font.AttributeValues;
import sun.font.FontUtilities;
import sun.font.StandardGlyphVector;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class FontMetricsPatch {

    public static Rectangle2D getStringBounds(Font font, String str, Graphics context) {
        char[] array = str.toCharArray();
        return getStringBounds(font, array, 0, array.length, myFRC(context));
    }

    // Below is mostly a copy of FontMetrics from jdk8 code, with some introspection to access Font.values fields.
    private static final FontRenderContext DEFAULT_FRC = new FontRenderContext(null, false, false);
    private static FontRenderContext myFRC(Graphics context) {
        if (context instanceof Graphics2D) {
            return ((Graphics2D)context).getFontRenderContext();
        }
        return DEFAULT_FRC;
    }

    private static Map<Font, AttributeValues> fontToValues = new HashMap<>();
    private static AttributeValues getValues(Font font) {
        AttributeValues values = fontToValues.get(font);
        if (values == null) {
            try {
                values = getFieldValue(Font.class, font, "values");
            } catch (IllegalAccessException e) {
                e.printStackTrace(System.err);
            }
            fontToValues.put(font, values);
        }
        return values;
    }

    public static <T> T getFieldValue(Class instanceClass, Object classInstance, String fieldName) throws IllegalAccessException {
        Field field = FieldUtils.getField(instanceClass, fieldName, true /*forceAccessible*/);
        return (T) FieldUtils.readField(field, classInstance, true /*forceAccess*/);
    }

    /**
     * Returns the logical bounds of the specified array of characters
     * in the specified <code>FontRenderContext</code>.  The logical
     * bounds contains the origin, ascent, advance, and height, which
     * includes the leading.  The logical bounds does not always enclose
     * all the text.  For example, in some languages and in some fonts,
     * accent marks can be positioned above the ascent or below the
     * descent.  To obtain a visual bounding box, which encloses all the
     * text, use the {@link TextLayout#getBounds() getBounds} method of
     * <code>TextLayout</code>.
     * <p>Note: The returned bounds is in baseline-relative coordinates
     * (see {@link Font class notes}).
     * @param chars an array of characters
     * @param beginIndex the initial offset in the array of
     * characters
     * @param limit the end offset in the array of characters
     * @param frc the specified <code>FontRenderContext</code>
     * @return a <code>Rectangle2D</code> that is the bounding box of the
     * specified array of characters in the specified
     * <code>FontRenderContext</code>.
     * @throws IndexOutOfBoundsException if <code>beginIndex</code> is
     *         less than zero, or <code>limit</code> is greater than the
     *         length of <code>chars</code>, or <code>beginIndex</code>
     *         is greater than <code>limit</code>.
     * @see FontRenderContext
     * @see Font#createGlyphVector
     * @since 1.2
     */
    private static Rectangle2D getStringBounds(Font font, char [] chars,
                                               int beginIndex, int limit,
                                               FontRenderContext frc) {

        final AttributeValues values = getValues(font);

        if (beginIndex < 0) {
            throw new IndexOutOfBoundsException("beginIndex: " + beginIndex);
        }
        if (limit > chars.length) {
            throw new IndexOutOfBoundsException("limit: " + limit);
        }
        if (beginIndex > limit) {
            throw new IndexOutOfBoundsException("range length: " +
                    (limit - beginIndex));
        }

        // this code should be in textlayout
        // quick check for simple text, assume GV ok to use if simple

        boolean simple = values == null ||
                (values.getKerning() == 0 && values.getLigatures() == 0 &&
                        values.getBaselineTransform() == null);
        if (simple) {
            simple = ! FontUtilities.isComplexText(chars, beginIndex, limit);
        }

        if (simple) {
            GlyphVector gv = new StandardGlyphVector(font, chars, beginIndex,
                    limit - beginIndex, frc);
            return gv.getLogicalBounds();
        } else {
            // need char array constructor on textlayout
            String str = new String(chars, beginIndex, limit - beginIndex);
            TextLayout tl = new TextLayout(str, font, frc);
            return new Rectangle2D.Float(0, -tl.getAscent(), tl.getAdvance(),
                    tl.getAscent() + tl.getDescent() +
                            tl.getLeading());
        }
    }
}

