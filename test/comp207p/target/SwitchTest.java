package comp207p.target;

import org.junit.Test;

import static org.junit.Assert.*;

public class SwitchTest {
    @Test
    public void foo() throws Exception {
        assertEquals(16032, new Switch().foo());
    }

}