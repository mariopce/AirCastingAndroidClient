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
package pl.llp.aircasting.model;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import pl.llp.aircasting.InjectedTestRunner;

import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

/**
 * Created by IntelliJ IDEA.
 * User: obrok
 * Date: 10/5/11
 * Time: 2:02 PM
 */
@RunWith(InjectedTestRunner.class)
public class SessionTest {
    @Inject Gson gson;

    Session session = new Session();
    Date date = new Date();
    Session emptySession = new Session();
    SoundMeasurement measurement = new SoundMeasurement(0, 0, 0);
    SoundMeasurement otherMeasurement = new SoundMeasurement(0, 0, 1);
    Note note = new Note();

    @Before
    public void setup() {
        session.add(measurement);
        session.add(otherMeasurement);
        session.add(note);
    }

    @Test
    public void shouldStoreSoundMeasurements() {
        assertThat(session.getSoundMeasurements(), hasItem(equalTo(measurement)));
    }

    @Test
    public void shouldProvideAvg() {
        assertThat(session.getAvg(), equalTo(0.5));
    }

    @Test
    public void shouldAllowSettingAvg() {
        emptySession.setAvg(1);
        assertThat(emptySession.getAvg(), equalTo(1.0));
    }

    @Test
    public void shouldProvidePeak() {
        assertThat(session.getPeak(), equalTo(1.0));
    }

    @Test
    public void shouldAllowSettingPeak() {
        emptySession.setPeak(1);
        assertThat(emptySession.getPeak(), equalTo(1.0));
    }

    @Test
    public void shouldProvideStartDate() {
        assertThat(session.getStart(), equalTo(measurement.getTime()));
    }

    @Test
    public void shouldAllowSettingStart() {
        emptySession.setStart(date);
        assertThat(emptySession.getStart(), equalTo(date));
    }

    @Test
    public void shouldProvideEndDate() {
        assertThat(session.getEnd(), equalTo(otherMeasurement.getTime()));
    }

    @Test
    public void shouldAllowSettingEnd() {
        emptySession.setEnd(date);
        assertThat(emptySession.getEnd(), equalTo(date));
    }

    @Test
    public void shouldUpdateAverage() {
        emptySession.getAvg();
        emptySession.add(otherMeasurement);
        assertThat(emptySession.getAvg(), equalTo(1.0));
    }

    @Test
    public void shouldUpdatePeak() {
        emptySession.getPeak();
        emptySession.add(otherMeasurement);
        assertThat(emptySession.getPeak(), equalTo(1.0));
    }

    @Test
    public void shouldExposeMeasurementsToGSON() {
        Session result = gson.fromJson(gson.toJson(session), Session.class);

        assertThat(result.getSoundMeasurements(), hasItem(measurement));
        assertThat(result.getSoundMeasurements(), hasItem(measurement));
    }

    @Test
    public void shouldHaveUUID(){
        assertThat(session.getUUID(), not(equalTo(null)));
        assertThat(session.getUUID(), not(equalTo(new Session().getUUID())));
    }

    @Test
    public void shouldAlwaysHaveMeasurements(){
        session.soundMeasurements = null;

        assertThat(session.getSoundMeasurements(), not(equalTo(null)));
    }
}
