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
package pl.llp.aircasting.activity;

import android.content.Intent;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import pl.llp.aircasting.InjectedTestRunner;
import pl.llp.aircasting.Intents;
import pl.llp.aircasting.R;
import pl.llp.aircasting.model.Session;
import pl.llp.aircasting.model.SessionManager;

import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: obrok
 * Date: 10/20/11
 * Time: 11:34 AM
 */
@RunWith(InjectedTestRunner.class)
public class AirCastingActivityEditSessionTest {
    @Inject AirCastingActivity activity;
    private Session session;
    private Intent data;

    @Before
    public void setup() {
        activity.sessionManager = mock(SessionManager.class);
        session = mock(Session.class);
        data = mock(Intent.class);
        when(activity.sessionManager.getSession()).thenReturn(session);
    }

    @Test
    public void shouldUpdateSessions() {
        when(data.getSerializableExtra(Intents.SESSION)).thenReturn(session);

        activity.onActivityResult(Intents.EDIT_SESSION, R.id.save_button, data);

        verify(activity.sessionManager).updateSession(session);
    }

    @Test
    public void shouldNotUpdateSessionOnCancel() {
        when(data.getSerializableExtra(Intents.SESSION)).thenThrow(new RuntimeException());

        activity.onActivityResult(Intents.EDIT_SESSION, R.id.discard_button, data);

        verify(activity.sessionManager, never()).updateSession(Mockito.any(Session.class));
    }
}
