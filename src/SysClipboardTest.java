import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by nelsons on 2/11/2017.
 */
public class SysClipboardTest {
    @Test
    public void roundTrip() throws Exception {
        SysClipboard.write("Unit test from ClipDashboard");
        String clip = SysClipboard.read();
        assertEquals(clip, "Unit test from ClipDashboard");
    }
}