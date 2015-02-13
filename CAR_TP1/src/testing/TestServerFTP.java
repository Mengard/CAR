/**
 * Created by William on 10/02/2015.
 */
package testing;

import junit.framework.Assert;
import org.testng.annotations.Test;


public class TestServerFTP {

    Client client = new Client(1779,1780);

    @Test()
    public void test() {
        client.sendMessage("USER Wyll");
        Assert.assertEquals("331 User name okay, need password", client.receiveMessage());
    }


}
