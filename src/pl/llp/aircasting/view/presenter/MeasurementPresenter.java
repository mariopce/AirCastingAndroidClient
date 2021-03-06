/**
 AirCasting - Share your Air!
 Copyright (C) 2011-2012 HabitatMap, Inc.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.

 You can contact the authors by email at <info@habitatmap.org>
 */
package pl.llp.aircasting.view.presenter;

import android.content.SharedPreferences;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import pl.llp.aircasting.helper.SettingsHelper;
import pl.llp.aircasting.model.Note;
import pl.llp.aircasting.model.SessionManager;
import pl.llp.aircasting.model.SoundMeasurement;

import java.util.*;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Multimaps.index;
import static java.util.Collections.sort;

/**
 * Created by IntelliJ IDEA.
 * User: obrok
 * Date: 10/4/11
 * Time: 1:47 PM
 */
@Singleton
public class MeasurementPresenter implements SessionManager.Listener, SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject SessionManager sessionManager;
    @Inject SettingsHelper settingsHelper;
    @Inject SharedPreferences preferences;

    @Inject
    public void init() {
        sessionManager.registerListener(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    private double anchor = 0;

    private LinkedList<SoundMeasurement> fullView = null;
    private int measurementsSize;

    private MeasurementAggregator aggregator = new MeasurementAggregator();

    private static final long MIN_ZOOM = 30000;
    private long zoom = MIN_ZOOM;

    private LinkedList<SoundMeasurement> timelineView;
    private List<Listener> listeners = newArrayList();

    @Override
    public synchronized void onNewMeasurement(SoundMeasurement measurement) {
        if (!sessionManager.isSessionSaved()) {
            prepareFullView();
            updateFullView(measurement);

            if (timelineView != null && (int) anchor == 0) {
                updateTimelineView();
            }
        }

        notifyListeners();
    }

    private void updateFullView(SoundMeasurement measurement) {
        boolean newBucket = isNewBucket(measurement);

        if (newBucket) {
            if (!aggregator.isEmpty()) {
                for (Listener listener : listeners) {
                    listener.onAveragedMeasurement(aggregator.getAverage());
                }
            }
            aggregator.reset();
        } else {
            fullView.removeLast();
        }

        aggregator.add(measurement);
        fullView.add(aggregator.getAverage());
    }

    private boolean isNewBucket(SoundMeasurement measurement) {
        if (fullView.isEmpty()) return true;

        SoundMeasurement last = getLast(fullView);

        return bucket(last) != bucket(measurement);
    }

    @Override
    public synchronized void onNewSession() {
        reset();

        notifyListeners();
    }

    private void reset() {
        fullView = null;
        timelineView = null;
        anchor = 0;
    }

    private void prepareFullView() {
        if (fullView != null) return;

        ImmutableListMultimap<Long, SoundMeasurement> forAveraging =
                index(sessionManager.getSoundMeasurements(), new Function<SoundMeasurement, Long>() {
                    @Override
                    public Long apply(SoundMeasurement measurement) {
                        return bucket(measurement);
                    }
                });

        ArrayList<Long> keys = newArrayList(forAveraging.keySet());
        sort(keys);
        fullView = newLinkedList();

        for (Long key : keys) {
            ImmutableList<SoundMeasurement> measurements = forAveraging.get(key);
            fullView.add(average(measurements));
        }
    }

    private long bucket(SoundMeasurement measurement) {
        return measurement.getTime().getTime() / (settingsHelper.getAveragingTime() * 1000);
    }

    private SoundMeasurement average(Collection<SoundMeasurement> measurements) {
        aggregator.reset();

        for (SoundMeasurement measurement : measurements) {
            aggregator.add(measurement);
        }

        return aggregator.getAverage();
    }

    @Override
    public void onNewNote(Note eq) {
    }

    @Override
    public void onNewReading() {
    }

    @Override
    public void onError() {
    }

    private void notifyListeners() {
        for (Listener listener : listeners) {
            listener.onViewUpdated();
        }
    }

    void setZoom(long zoom) {
        this.zoom = zoom;
        timelineView = null;
        notifyListeners();
    }

    public List<SoundMeasurement> getTimelineView() {
        if (sessionManager.isSessionSaved() || sessionManager.isSessionStarted()) {
            prepareTimelineView();
            return timelineView;
        } else {
            return new ArrayList<SoundMeasurement>();
        }
    }

    protected synchronized void prepareTimelineView() {
        if (timelineView != null) return;

        final List<SoundMeasurement> measurements = getFullView();
        int position = measurements.size() - 1 - (int) anchor;
        final long last = measurements.isEmpty()
                ? 0 : measurements.get(position).getTime().getTime();

        timelineView = newLinkedList(filter(measurements, new Predicate<SoundMeasurement>() {
            @Override
            public boolean apply(SoundMeasurement measurement) {
                return measurement.getTime().getTime() <= last &&
                        last - measurement.getTime().getTime() <= zoom;
            }
        }));

        measurementsSize = measurements.size();
    }

    private synchronized void updateTimelineView() {
        SoundMeasurement measurement = aggregator.getAverage();

        if (aggregator.isComposite()) {
            if (!timelineView.isEmpty()) timelineView.removeLast();
            timelineView.add(measurement);
        } else {
            while (timelineView.size() > 0 &&
                    measurement.getTime().getTime() - timelineView.get(0).getTime().getTime() >= zoom) {
                timelineView.removeFirst();
            }
            measurementsSize += 1;
            timelineView.add(measurement);
        }
    }

    public void registerListener(Listener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

    public boolean canZoomIn() {
        return zoom > MIN_ZOOM;
    }

    public boolean canZoomOut() {
        prepareTimelineView();
        return timelineView.size() < measurementsSize;
    }

    public void zoomIn() {
        if (canZoomIn()) {
            setZoom(zoom / 2);
        }
    }

    public void zoomOut() {
        if (canZoomOut()) {
            anchor -= timelineView.size();
            fixAnchor();
            setZoom(zoom * 2);
        }
    }

    public List<SoundMeasurement> getFullView() {
        if (sessionManager.isSessionSaved() || sessionManager.isSessionStarted()) {
            prepareFullView();
            return fullView;
        } else {
            return new ArrayList<SoundMeasurement>();
        }
    }

    public synchronized void scroll(double scroll) {
        prepareTimelineView();

        anchor -= scroll * timelineView.size();
        fixAnchor();
        timelineView = null;

        notifyListeners();
    }

    private void fixAnchor() {
        if (anchor > measurementsSize - timelineView.size()) {
            anchor = measurementsSize - timelineView.size();
        }
        if (anchor < 0) {
            anchor = 0;
        }
    }

    public boolean canScrollRight() {
        return anchor != 0;
    }

    public boolean canScrollLeft() {
        prepareTimelineView();
        return anchor < measurementsSize - timelineView.size();
    }

    @Override
    public synchronized void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (key.equals(SettingsHelper.AVERAGING_TIME)) {
            reset();
            notifyListeners();
        }
    }

    public double getLastAveraged() {
        if(fullView == null || fullView.isEmpty()){
            return 0;
        } else {
            return getLast(fullView).getValue();
        }
    }

    public interface Listener {
        void onViewUpdated();

        void onAveragedMeasurement(SoundMeasurement measurement);
    }

    private class MeasurementAggregator {
        private double longitude = 0;
        private double latitude = 0;
        private double value = 0;
        private long time = 0;
        private int count = 0;

        public void add(SoundMeasurement measurement) {
            longitude += measurement.getLongitude();
            latitude += measurement.getLatitude();
            value += measurement.getValue();
            time += measurement.getTime().getTime();
            count += 1;
        }

        public void reset() {
            longitude = latitude = value = time = count = 0;
        }

        public SoundMeasurement getAverage() {
            return new SoundMeasurement(latitude / count, longitude / count, value / count, new Date(time / count));
        }

        public boolean isComposite() {
            return count > 1;
        }

        public boolean isEmpty() {
            return count == 0;
        }
    }
}
