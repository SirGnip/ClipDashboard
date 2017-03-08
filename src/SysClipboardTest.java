import org.junit.Test;

import static org.junit.Assert.*;

public class SysClipboardTest {
    @Test
    public void roundTrip() throws Exception {
        SysClipboard.write("Unit test from ClipDashboard");
        String clip = SysClipboard.read();
        assertEquals(clip, "Unit test from ClipDashboard");
    }
}