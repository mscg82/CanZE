/*
    CanZE
    Take a closer look at your ZE car

    Copyright (C) 2015 - The CanZE Team
    http://canze.fisch.lu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or any
    later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package lu.fisch.canze.widgets;

import android.content.res.Resources;
import android.util.TypedValue;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import lu.fisch.awt.Color;
import lu.fisch.awt.Graphics;
import lu.fisch.awt.Polygon;
import lu.fisch.canze.activities.MainActivity;
import lu.fisch.canze.actors.Field;
import lu.fisch.canze.actors.Fields;
import lu.fisch.canze.classes.TimePoint;
import lu.fisch.canze.database.CanzeDataSource;
import lu.fisch.canze.interfaces.DrawSurfaceInterface;

/**
 * @author robertfisch
 */
public class Timeplot extends Drawable {

    protected HashMap<String, ArrayList<TimePoint>> values = new HashMap<>();

    public Timeplot() {
        super();
    }

    public Timeplot(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        // test
    }

    public Timeplot(DrawSurfaceInterface drawSurface, int x, int y, int width, int height) {
        this.drawSurface = drawSurface;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void addValue(String fieldSID, double value) {
        long iTime = Calendar.getInstance().getTimeInMillis();
        // with the new dongle, fields may come in too fast, so let's
        // make sure we do not get an overflow >> very slow app reaction
        // maximum each second a new value!
        iTime = (iTime / 1000) * 1000;

        //MainActivity.debug(values.size()+"");
        if (!values.containsKey(fieldSID)) values.put(fieldSID, new ArrayList<TimePoint>());

        // don't add every point, but check if for the given second we already have this point
        // remembering more than one point per second is kind of overkill
        // values.get(fieldSID).add(new TimePoint(Calendar.getInstance().getTimeInMillis(), value));

        // if empty, add

        /* TODO  given crashlitics, I think we should sync this block JM */
        ArrayList<TimePoint> val = values.get(fieldSID);
        if (val == null) return;
        if (val.size() == 0)
            val.add(new TimePoint(iTime, value));
        else {
            TimePoint lastTP = val.get(val.size() - 1);
            // if this is really a new point, add it
            if (lastTP == null || lastTP.date != iTime)
                val.add(new TimePoint(iTime, value));
                // if not, replace the previous point
                // ( database will store the max, but as the value of the last point is also being
                //   displayed on the screen, we should prefer having the real last point here )
            else {
                val.set(val.size() - 1, new TimePoint(iTime, value));
            }
        }
    }

    private Color getColor(int i) {
        switch (i) {
            case 0:
                return Color.RENAULT_RED;

            case 1:
                return Color.WHITE;

            case 2:
                return Color.GREEN;

            default:
                return Color.RENAULT_BLUE;
        }
    }

    @Override
    public void draw(Graphics g) {
        // background
        g.setColor(getBackground());
        g.fillRect(x, y, width, height);

        // black border
        g.setColor(getForeground());
        g.drawRect(x, y, width, height);

        // calculate fill height
        //int fillHeight = (int) ((value-min)/(double)(max-min)*(height-1));
        int barWidth = width - Math.max(g.stringWidth(String.valueOf(min)), g.stringWidth(String.valueOf(max))) - 10 - 10;
        int spaceAlt = Math.max(g.stringWidth(String.valueOf(minAlt)), g.stringWidth(String.valueOf(maxAlt))) + 10 + 10;
        // reduce with if second y-axe is used
        //MainActivity.debug("Alt: "+minAlt+" - "+maxAlt);
        if (minAlt == 0 && maxAlt == 0) {
            spaceAlt = 0;
        }
        barWidth -= spaceAlt;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        int graphHeight = height - g.stringHeight(sdf.format(Calendar.getInstance().getTime())) - 15;

        // draw the ticks
        double realMaxAlt = getMaxAlt();
        if (minorTicks > 0 || majorTicks > 0) {
            int toTicks = minorTicks;
            if (toTicks == 0) toTicks = majorTicks;
            double intervals = ((max - min) / (double) toTicks);
            double accel = (double) graphHeight / intervals;
            double ax, ay, bx, by;
            int actual = min;
            int actualAlt = minAlt;
            int sum = 0;
            for (double i = graphHeight; i >= 0; i -= accel) {
                if (minorTicks > 0) {
                    g.setColor(getForeground());
                    ax = x + width - barWidth - spaceAlt - 5;
                    ay = y + i;
                    bx = x + width - barWidth - spaceAlt;
                    by = y + i;
                    g.drawLine((int) ax, (int) ay, (int) bx, (int) by);

                    if (spaceAlt != 0) {
                        ax = x + width - spaceAlt;
                        ay = y + i;
                        bx = ax + 5;
                        by = y + i;
                        g.drawLine((int) ax, (int) ay, (int) bx, (int) by);
                    }
                }
                // draw majorTicks
                if (majorTicks != 0 && sum % majorTicks == 0) {
                    if (majorTicks > 0) {
                        g.setColor(getIntermediate());
                        ax = x + width - spaceAlt - barWidth - 10;
                        ay = y + i;
                        bx = x + width - spaceAlt + (spaceAlt != 0 ? 10 : 0);
                        by = y + i;
                        g.drawLine((int) ax, (int) ay, (int) bx, (int) by);

                        g.setColor(getForeground());
                        ax = x + width - barWidth - spaceAlt - 10;
                        ay = y + i;
                        bx = x + width - barWidth - spaceAlt;
                        by = y + i;
                        g.drawLine((int) ax, (int) ay, (int) bx, (int) by);

                        if (spaceAlt != 0) {
                            ax = x + width - spaceAlt;
                            ay = y + i;
                            bx = ax + 10;
                            by = y + i;
                            g.drawLine((int) ax, (int) ay, (int) bx, (int) by);
                        }
                    }

                    // draw String
                    if (showLabels) {
                        g.setColor(getForeground());
                        g.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, Resources.getSystem().getDisplayMetrics()));
                        String text = String.valueOf(actual);
                        double sw = g.stringWidth(text);
                        bx = x + width - barWidth - 16 - sw - spaceAlt;
                        by = y + i;
                        g.drawString(text, (int) (bx), (int) (by + g.stringHeight(text) * (1 - i / graphHeight)));

                        // alternative labels
                        if (spaceAlt != 0) {
                            text = String.valueOf(actualAlt);
                            //sw = g.stringWidth(text);
                            bx = x + width - spaceAlt + 16;
                            by = y + i;
                            g.drawString(text, (int) (bx), (int) (by + g.stringHeight(text) * (1 - i / graphHeight)));
                        }
                    }

                    actual += majorTicks;
                    actualAlt += Math.round((double) (maxAlt - minAlt) / (max - min) * majorTicks);
                }
                sum += minorTicks;
            }
            // calculate real max alt
            double altTicks = Math.round((double) (maxAlt - minAlt) / (max - min) * majorTicks) / ((double) majorTicks / toTicks);
            realMaxAlt = intervals * altTicks + getMinAlt();
        }

        // draw the vertical grid
        g.setColor(getIntermediate());
        long start = (Calendar.getInstance().getTimeInMillis() / 1000); // start in seconds
        int interval = 60 / timeScale;

        try {
            if (!isInverted()) {
                ArrayList<TimePoint> list = getTimePointsForSidOrFail(sids.get(0));
                start = list.get(list.size() - 1).date / 1000;
                for (int s = 0; s < sids.size(); s++) {
                    list = getTimePointsForSidOrFail(sids.get(s));
                    long thisDate = list.get(list.size() - 1).date / 1000;
                    if (thisDate > start) start = thisDate;
                }
                long newStart = start;
                for (long x = width - (newStart % interval) - spaceAlt; x >= width - barWidth - spaceAlt; x -= interval) {
                    g.drawLine(x, 1, x, graphHeight + 5);
                }
            } else {
                ArrayList<TimePoint> list = getTimePointsForSidOrFail(sids.get(0));
                start = list.get(0).date / 1000;
                for (int s = 0; s < sids.size(); s++) {
                    list = getTimePointsForSidOrFail(sids.get(s));
                    long thisDate = list.get(0).date / 1000;
                    if (thisDate < start) start = thisDate;
                }
                for (long x = width - barWidth - spaceAlt; x < width - spaceAlt; x += interval) {
                    g.drawLine(x, 1, x, graphHeight + 5);
                }
            }
        } catch (Exception e) {
            //MainActivity.debug("Exception: "+e.getMessage());
            for (long x = width - (start % interval) - spaceAlt; x >= width - barWidth - spaceAlt; x -= interval) {
                g.drawLine(x, 1, x, graphHeight + 5);
            }
        }

        // draw the graph
        for (int s = 0; s < sids.size(); s++) {
            String sid = sids.get(s);

            ArrayList<TimePoint> tmpValues;
            // setup an empty list if no list has been found
            ArrayList<TimePoint> values = getTimePointsForSid(sid);
            if (values == null) {
                tmpValues = new ArrayList<>();
                this.values.put(sid, tmpValues);
            } else {
                // make a shallow copy. this avoids array size changes and thus out of bounds while drawing
                tmpValues = new ArrayList<>(values);
            }

            g.setColor(getForeground());
            g.drawRect(x + width - barWidth - spaceAlt, y, barWidth, graphHeight);
            if (!tmpValues.isEmpty()) {

                double w = (double) barWidth / tmpValues.size();
                double h = (double) graphHeight / (getMax() - getMin());
                double hAlt = (double) graphHeight / (realMaxAlt - getMinAlt());

                double lastX = Double.NaN;
                double lastY = Double.NaN;
                g.setColor(getColor(s));

                String options = getOptions().getOption(sid);
                if (!isInverted()) {

                    long maxTime = start * 1000; //values.get(values.size() - 1).date;

                    for (int i = tmpValues.size() - 1; i >= 0; i--) {
                        TimePoint tp = getTimePoint(tmpValues, i);

                        if (tp != null && !Double.isNaN(tp.value) && tp.date != 0) {
                            g.setColor(colorRanges.getColor(sid, tp.value, getColor(s)));

                            double mx = barWidth - (((double) (maxTime - tp.date)) / timeScale / 1000.0);

                            if (mx < 0) {
                                tmpValues.remove(i);
                            } else {
                                // determine Y
                                double my = computeY(tp, h, hAlt, graphHeight, options);

                                // now get ZY
                                double zy = computeZY(h, hAlt, graphHeight, options);

                                int rayon = 2;

                                if (shouldPrintADot(options)) {
                                    g.fillOval(getX() + getWidth() - barWidth + (int) mx - rayon - spaceAlt,
                                            getY() + (int) my - rayon,
                                            2 * rayon + 1,
                                            2 * rayon + 1);
                                }

                                if (i < tmpValues.size() - 1 && (mx != 0 || my != 0)) {
                                    if (optionsContains(options, "full")) {
                                        if (!testErrorPoint(lastX, lastY, "last full") && !testErrorPoint(mx, my, "m full")) {
                                            Polygon p = new Polygon();
                                            p.addPoint(getX() + getWidth() - barWidth + (int) lastX - spaceAlt,
                                                    getY() + (int) lastY);
                                            p.addPoint(getX() + getWidth() - barWidth + (int) mx - spaceAlt,
                                                    getY() + (int) my);
                                            p.addPoint(getX() + getWidth() - barWidth + (int) mx - spaceAlt,
                                                    (int) (getY() + zy));
                                            p.addPoint(getX() + getWidth() - barWidth + (int) lastX - spaceAlt,
                                                    (int) (getY() + zy));
                                            g.fillPolygon(p);
                                        }
                                    } else if (optionsContains(options, "gradient")) {

                                        if (i < tmpValues.size() && tmpValues.get(i + 1) != null) {
                                            if (!testErrorPoint(lastX, lastY, "last grad") && !testErrorPoint(mx, my, "m grad")) {
                                                Polygon p = new Polygon();
                                                p.addPoint(getX() + getWidth() - barWidth + (int) lastX - spaceAlt,
                                                        getY() + (int) lastY);
                                                p.addPoint(getX() + getWidth() - barWidth + (int) mx - spaceAlt,
                                                        getY() + (int) my);
                                                p.addPoint(getX() + getWidth() - barWidth + (int) mx - spaceAlt,
                                                        (int) (getY() + zy));
                                                p.addPoint(getX() + getWidth() - barWidth + (int) lastX - spaceAlt,
                                                        (int) (getY() + zy));

                                                int[] colors = colorRanges.getColors(sid);
                                                float[] spacings = colorRanges.getSpacings(sid, min, max);
                                                if (colors.length == spacings.length)
                                                    g.setGradient(0, graphHeight, 0, 0, colors, spacings);
                                                g.fillPolygon(p);
                                                g.clearGradient();

                                                //else MainActivity.debug("size not equal: "+colors.length+"=="+spacings.length);
                                            }
                                        }
                                    } else {
                                        if (!testErrorPoint(mx, my, "m line")) {
                                            g.drawLine(getX() + getWidth() - barWidth + (int) lastX - spaceAlt,
                                                    getY() + (int) lastY,
                                                    getX() + getWidth() - barWidth + (int) mx - spaceAlt,
                                                    getY() + (int) my);
                                        }
                                    }
                                }
                                lastX = mx;
                                lastY = my;
                            }
                        }
                    }
                } else { // forward
                    long minTime = start * 1000; //values.get(0).date;

                    for (int i = 0; i < tmpValues.size(); i++) {
                        TimePoint tp = getTimePoint(tmpValues, i);

                        if (tp != null && !Double.isNaN(tp.value) && tp.date != 0) {
                            g.setColor(colorRanges.getColor(sid, tp.value, getColor(s)));

                            double mx = (((double) (tp.date - minTime)) / timeScale / 1000.0);

                            if (mx <= barWidth) {
                                // ignore point that are out of scope but do not delete them
                                double my = computeY(tp, h, hAlt, graphHeight, options);

                                // now get ZY
                                double zy = computeZY(h, hAlt, graphHeight, options);

                                int rayon = 2;

                                //MainActivity.debug("HERE: "+sid+" / "+getOptions().getOption(sid));

                                if (shouldPrintADot(options)) {
                                    g.fillOval(getX() + getWidth() - barWidth + (int) mx - rayon - spaceAlt,
                                            getY() + (int) my - rayon,
                                            2 * rayon + 1,
                                            2 * rayon + 1);
                                }

                                if (i > 0 && (mx != 0 || my != 0)) {
                                    if (optionsContains(options, "full")) {
                                        if (!testErrorPoint(lastX, lastY, "last full") && !testErrorPoint(mx, my, "m full")) {
                                            Polygon p = new Polygon();
                                            p.addPoint(getX() + getWidth() - barWidth + (int) lastX - spaceAlt,
                                                    getY() + (int) lastY);
                                            p.addPoint(getX() + getWidth() - barWidth + (int) mx - spaceAlt,
                                                    getY() + (int) my);
                                            p.addPoint(getX() + getWidth() - barWidth + (int) mx - spaceAlt,
                                                    (int) (getY() + zy));
                                            p.addPoint(getX() + getWidth() - barWidth + (int) lastX - spaceAlt,
                                                    (int) (getY() + zy));
                                            g.fillPolygon(p);
                                        }
                                    } else if (optionsContains(options, "gradient")) {

                                        //if (i < values.size() && values.get(i - 1) != null) {
                                        if (!testErrorPoint(lastX, lastY, "last grad") && !testErrorPoint(mx, my, "m grad")) {
                                            Polygon p = new Polygon();
                                            p.addPoint(getX() + getWidth() - barWidth + (int) lastX - spaceAlt,
                                                    getY() + (int) lastY);
                                            p.addPoint(getX() + getWidth() - barWidth + (int) mx - spaceAlt,
                                                    getY() + (int) my);
                                            p.addPoint(getX() + getWidth() - barWidth + (int) mx - spaceAlt,
                                                    (int) (getY() + zy));
                                            p.addPoint(getX() + getWidth() - barWidth + (int) lastX - spaceAlt,
                                                    (int) (getY() + zy));

                                            int[] colors = colorRanges.getColors(sid);
                                            float[] spacings = colorRanges.getSpacings(sid, min, max);
                                            if (colors.length == spacings.length)
                                                g.setGradient(0, graphHeight, 0, 0, colors, spacings);
                                            g.fillPolygon(p);
                                            g.clearGradient();

                                            //else MainActivity.debug("size not equal: "+colors.length+"=="+spacings.length);
                                        }
                                        //}
                                    } else {
                                        if (!testErrorPoint(mx, my, "m line")) {
                                            g.drawLine(getX() + getWidth() - barWidth + (int) lastX - spaceAlt,
                                                    getY() + (int) lastY,
                                                    getX() + getWidth() - barWidth + (int) mx - spaceAlt,
                                                    getY() + (int) my);
                                        }
                                    }
                                }
                                lastX = mx;
                                lastY = my;
                            }
                        }
                    }
                }
            }
        }

        // clean bottom
        g.setColor(getBackground());
        g.fillRect(width - barWidth - 2, graphHeight + 1, barWidth + 1, height - graphHeight - 2);

        // draw bottom axis
        int c = 0;
        int ts = (int) timeScale;

        // draw the horizontal scale
        sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        if (!this.values.isEmpty())
            if (!isInverted()) {
                long newStart = (Calendar.getInstance().getTimeInMillis()); // use now as starting
                for (int s = 0; s < sids.size(); s++) { //check all sids in this graph
                    ArrayList<TimePoint> list = getTimePointsForSid(sids.get(s)); // get the timepoints of this sid
                    if (list != null && !list.isEmpty()) {
                        TimePoint tp = list.get(list.size() - 1); // get the last one
                        if (tp != null) {
                            if (tp.date > newStart) newStart = tp.date; // find the max
                        }
                    }
                }
                //long newStart = start*1000;
                //MainActivity.debug("Start 2: "+sdf.format(newStart));
                for (long x = width - (start % interval) - spaceAlt; x >= width - barWidth - spaceAlt; x -= interval) {
                    if (c % (5 * ts) == 0) {
                        g.setColor(getForeground());
                        g.drawLine(x, graphHeight, x, graphHeight + 10);
                        String date = sdf.format((newStart - ((newStart % interval)) * timeScale - interval * c * timeScale * 1000L));
                        g.drawString(date, x - g.stringWidth(date) - 4, height - 2);
                    } else {
                        g.setColor(getForeground());
                        g.drawLine(x, graphHeight, x, graphHeight + 3);
                    }
                    c++;
                }
            } else { // forward
                ArrayList<TimePoint> list; // = this.values.get(sids.get(0));
                long newStart = (Calendar.getInstance().getTimeInMillis());
                for (int s = 0; s < sids.size(); s++) { //check all sids in this graph
                    list = getTimePointsForSid(sids.get(s)); // get the timepoints of this sid
                    if (list != null && !list.isEmpty()) {
                        TimePoint tp = list.get(0); // get the first one
                        if (tp != null) {
                            if (tp.date < newStart) newStart = tp.date; // find the min
                        }
                    }
                }
                //MainActivity.debug("START: "+sdf.format(start));
                //long newStart = start*1000;
                for (long x = width - barWidth - spaceAlt; x < width - spaceAlt; x += interval) {
                    if (c % (5 * ts) == 0) {
                        g.setColor(getForeground());
                        g.drawLine(x, graphHeight, x, graphHeight + 10);
                        String date = sdf.format(newStart + (interval * c * timeScale * 1000L));
                        g.drawString(date, x - g.stringWidth(date) - 4, height - 2);
                    } else {
                        g.setColor(getForeground());
                        g.drawLine(x, graphHeight, x, graphHeight + 3);
                    }
                    c++;
                }
            }


        // draw the title
        if (title != null && !title.equals("")) {
            g.setColor(getTitleColor());
            Sizes sizes = new Sizes(getOptions().getOption("titleSize"), 14, 10);
            g.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizes.getSize(), Resources.getSystem().getDisplayMetrics()));

            // draw multi-line title
            title = title.replace(" / ", ", "); // let's be lazy
            String[] parts = title.split(",");
            int tx = getX() + width - barWidth + 8 - spaceAlt;
            for (int s = 0; s < sids.size(); s++) {
                String sid = sids.get(s);
                //ArrayList<TimePoint> tmpValues = this.values.get(sid);tmpValues =
                ArrayList<TimePoint> values = getTimePointsForSid(sid);
                if (values == null) continue;
                ArrayList<TimePoint> tmpValues = new ArrayList<>(values);

                int th = g.stringHeight(title);
                int ty = getY() + th + 4;

                if (tmpValues.isEmpty())
                    g.setColor(getColor(s));
                else
                    g.setColor(colorRanges.getColor(sid, tmpValues.get(tmpValues.size() - 1).value, getColor(s)));

                if (s < parts.length) {
                    g.drawString(parts[s].trim(), tx, ty);
                    tx += g.stringWidth(parts[s].trim());
                }

                // draw " / " if needed
                if (s < sids.size() - 1) {
                    g.setColor(getTitleColor());
                    g.drawString(" / ", tx, ty);
                    tx += g.stringWidth(" /-"); // " / " will _not_ work!
                }
            }

            /*
            // draw single line title
            int th = g.stringHeight(title);
            int tx = getX()+width-barWidth+8-spaceAlt;
            int ty = getY()+th+4;
            g.drawString(title,tx,ty);
            */
        }

        // draw the value
        if (showValue) {
            Sizes sizes = new Sizes(getOptions().getOption("valueSize"), 22, 18);
            g.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizes.getSize(), Resources.getSystem().getDisplayMetrics()));

            int tx = getX() + width - 8 - spaceAlt;
            int ty = getY();
            int dy = g.stringHeight("Ip") + 4; // full height and undersling
            for (int s = 0; s < sids.size(); s++) {
                String sid = sids.get(s);
                // ArrayList<TimePoint> tmpValues = this.values.get(sid);
                ArrayList<TimePoint> values = getTimePointsForSid(sid);
                if (values == null) continue;
                ArrayList<TimePoint> tmpValues = new ArrayList<>(values);

                Field field = Fields.getInstance().getBySID(sid);

                String text;

                if (field != null) {
                    //noinspection MalformedFormatString
                    text = String.format("%." + field.getDecimals() + "f", field.getValue());
                } else {
                    if (tmpValues.size() == 0) text = "N/A";
                    else text = String.valueOf(tmpValues.get(tmpValues.size() - 1).value);
                }

                if (tmpValues.isEmpty()) {
                    g.setColor(getColor(s));
                } else {
                    g.setColor(colorRanges.getColor(sid, tmpValues.get(tmpValues.size() - 1).value, getColor(s)));
                }

                int tw = g.stringWidth(text);
                ty += dy;
                g.drawString(text, tx - tw, ty);
            }
        }

        // black border
        g.setColor(getForeground());
        g.drawRect(x, y, width, height);
    }

    @Nullable
    private ArrayList<TimePoint> getTimePointsForSid(String sid) {
        return this.values.get(sid);
    }

    private ArrayList<TimePoint> getTimePointsForSidOrFail(String sid) {
        return Objects.requireNonNull(getTimePointsForSid(sid), "Unable to load time-points for sid " + sid);
    }

    private double computeY(TimePoint tp, double h, double hAlt, int graphHeight, String options) {
        // check if y should be fixed: colorline[value-of-y]
        if (optionsContains(options, "colorline")) {
            // parse out position of line
            double value = getValueForColorline(options);
            if (optionsContains(options, "alt")) {
                return graphHeight - (value - minAlt) * hAlt;
            } else {
                return graphHeight - (value - min) * h;
            }
        } else {
            // distinct "alt" vs "normal"
            if (optionsContains(options, "alt")) {
                return graphHeight - (tp.value - minAlt) * hAlt;
            } else {
                return graphHeight - (tp.value - min) * h;
            }
        }
    }

    private double computeZY(double h, double hAlt, int graphHeight, String options) {
        if (optionsContains(options, "alt")) {
            return graphHeight - (-minAlt) * hAlt;
        } else {
            return graphHeight - (-min) * h;
        }
    }

    private boolean shouldPrintADot(String options) {
        return options == null || options.isEmpty() || options.contains("dot");
    }

    @Nullable
    private TimePoint getTimePoint(ArrayList<TimePoint> tmpValues, int i) {
        try {
            return tmpValues.get(i);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private boolean optionsContains(String options, String full) {
        return options != null && options.contains(full);
    }

    private double getValueForColorline(String options) {
        int index = options.indexOf("colorline");
        index += ("colorline").length() + 1;
        int startIndex = index;
        while (options.charAt(index) != ']') {
            index++;
        }
        String valueStr = options.substring(startIndex, index);
        return Double.parseDouble(valueStr);
    }

    @Override
    public void onFieldUpdateEvent(Field field) {
        addValue(field.getSID(), field.getValue());

        super.onFieldUpdateEvent(field);
    }

    @Override
    public String dataToJson() {
        Gson gson = new Gson();
        return gson.toJson(values.clone());
    }

    @Override
    public void dataFromJson(String json) {
        Gson gson = new Gson();
        Type fooType = new TypeToken<HashMap<String, ArrayList<TimePoint>>>() {
        }.getType();

        values = gson.fromJson(json, fooType);
    }

    @Override
    public void loadValuesFromDatabase() {
        super.loadValuesFromDatabase();

        //values.clear(); // not needed as items will be replaced anyway!
        for (int s = 0; s < sids.size(); s++) {
            String sid = sids.get(s);
            values.put(sid, CanzeDataSource.getInstance().getData(sid));
        }
    }

    public void addField(String sid) {
        super.addField(sid);
        if (!values.containsKey(sid)) {
            values.put(sid, new ArrayList<TimePoint>());
        }
    }

    public void setValues(HashMap<String, ArrayList<TimePoint>> values) {
        sids.clear();

        sids.addAll(values.keySet());

        this.values = values;
    }

    /** @noinspection BooleanMethodIsAlwaysInverted*/
    private boolean testErrorPoint(double x, double y, String er) {
        double maxdelta = 2.0;
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return true;
        }
        // test for origin (close to 0, 0 to find triangle problem)
        // MainActivity.toast ("x:" + x + ", y:" + y + ", " + er);
        return x >= -maxdelta && x <= maxdelta && y >= -maxdelta && y <= maxdelta;
    }

    private static class Sizes {
        private final int landscape;
        private final int portrait;

        public Sizes(String source, int defaultLandscape, int defaultPortrait) {
            if (source != null && !source.trim().isEmpty()) {
                String[] parts = source.split(",");
                if (parts.length == 2) {
                    landscape = Integer.parseInt(parts[0].trim());
                    portrait = Integer.parseInt(parts[1].trim());
                    return;
                }
            }

            landscape = defaultLandscape;
            portrait = defaultPortrait;
        }

        public int getSize() {
            return MainActivity.getInstance().isLandscape() ? landscape : portrait;
        }
    }
}
