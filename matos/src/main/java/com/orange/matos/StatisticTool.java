
package com.orange.matos;

/*
 * #%L
 * Matos
 * %%
 * Copyright (C) 2004 - 2014 Orange SA
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import com.orange.matos.core.Out;
import com.orange.matos.core.Step;

/**
 * To create statistic charts
 * 
 * @author piac6784 (first version using jfree : vapu8214)
 */
public class StatisticTool {

    /**
     * Quality index for generated file. There are already a few artefacts but
     * they are acceptable.
     */
    private static final float JPEG_QUALITY = 0.7f;

    /**
     * The campaign which corresponds to steps in the current check-list.
     */
    private Campaign campaign;

    /**
     * WIDTH is HEIGHT + room for legend on the right.
     */
    private static final int WIDTH = 800;

    /**
     * HEIGHT in pixels
     */
    private static final int HEIGHT = 500;

    /**
     * Border for the disk
     */
    private static final int PIE_BORDER = 10;

    /**
     * Space between rows and pictures of legend.
     */
    private static final int DELTA_LEGEND = 25;

    /**
     * Font size
     */
    private static final int FONT_HEIGHT = 20;

    private static final int SZ = HEIGHT - 2 * PIE_BORDER;

    /**
     * Instantiates a new statistic tool.
     * 
     * @param campaign the campaign
     */
    public StatisticTool(Campaign campaign) {
        this.campaign = campaign;

    }

    static class Slice {
        final double value;

        final Color color;

        final String label;

        public Slice(double value, Color color, String label) {
            this.value = value;
            this.color = color;
            this.label = label;
        }
    }

    private static BufferedImage createImage(Slice[] slices) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, FONT_HEIGHT));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        double total = 0.0D;
        for (Slice slice : slices) {
            total += slice.value;
        }
        double curValue = 0.0D;
        int y = DELTA_LEGEND * 3;
        for (Slice slice : slices) {
            int startAngle = (int) (450 - (curValue * 360 / total)) % 360;
            int arcAngle = (int) (-slice.value * 360 / total);
            g.setColor(slice.color);
            g.fillArc(PIE_BORDER, PIE_BORDER, SZ, SZ, startAngle, arcAngle);
            g.fillRect(HEIGHT + DELTA_LEGEND, y - DELTA_LEGEND, DELTA_LEGEND, DELTA_LEGEND);
            g.setColor(Color.BLACK);
            g.drawString(slice.label, HEIGHT + 3 * DELTA_LEGEND, y);
            curValue += slice.value;
            y += 2 * DELTA_LEGEND;
        }
        g.dispose();
        return image;
    }

    /**
     * Creates data set with percentage of passed, failed, skipped and not
     * analysed.
     * 
     * @return the data set
     */
    private Slice[] createSampleDataset() {
        Slice[] result = new Slice[4];
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        int notAnalyzed = 0;
        for (int i = 0; i < campaign.size(); i++) {
            Step cmdLine = (Step) campaign.get(i);
            if (cmdLine.getVerdict() == Step.PASSED) {
                passed++;
            } else if (cmdLine.getVerdict() == Step.FAILED) {
                failed++;
            } else if (cmdLine.getVerdict() == Step.SKIPPED) {
                skipped++;
            } else if (cmdLine.getVerdict() == Step.NONE) {
                notAnalyzed++;
            }
        }
        int total = passed + failed + skipped + notAnalyzed;
        result[0] = new Slice(Double.valueOf(passed * 100. / total), Color.GREEN, "Passed");
        result[0] = new Slice(Double.valueOf(failed * 100. / total), Color.RED, "Failed");
        result[0] = new Slice(Double.valueOf(skipped * 100. / total), Color.ORANGE, "Skipped");
        result[0] = new Slice(Double.valueOf(notAnalyzed * 100. / total), Color.GRAY,
                "Not analyzed");
        return result;
    }

    /**
     * Creates a jpeg file with the generated chart
     * 
     * @param outFile the path to the output file
     */
    public void createJPEGFile(File outFile) {
        Slice[] slices = createSampleDataset();
        BufferedImage image = createImage(slices);
        try {
            saveToFile(image, outFile);
        } catch (IOException e) {
            Out.getLog().println("Cannot create statistic image " + outFile);
        }
    }

    private static void saveToFile(BufferedImage img, File file) throws IOException {
        ImageWriter writer = null;
        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpg");
        if (!iter.hasNext())
            return;
        writer = (ImageWriter) iter.next();

        ImageOutputStream ios = ImageIO.createImageOutputStream(file);
        if (ios == null) throw new IOException("cannot create stream for image to " + file);
        try {
            writer.setOutput(ios);
            ImageWriteParam param = new JPEGImageWriteParam(Locale.getDefault());
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
            ios.close();
        }
    }

    /**
     * Test de l'API de génération de fichier
     * 
     * @param args
     */
    public static void main(String args[]) {
        File outFile = new File("/tmp/aux.jpg");
        Slice[] slices = new Slice[] {
                new Slice(20, Color.GREEN, "Passed"), new Slice(30, Color.RED, "Failed"),
                new Slice(50, Color.ORANGE, "Skipped"), new Slice(0, Color.GRAY, "Not analyzed")
        };
        BufferedImage image = createImage(slices);
        try {
            saveToFile(image, outFile);
        } catch (IOException e) {
            // TODO
        }
    }
}
