package generated;

import org.junit.Test;

public class ShipOrderTest {
	@Test
	public void test() {
		ShipOrder shiporder = new ShipOrder();
		shiporder.withShipTo().setAddress("AAA");
		shiporder.withItem(2).setNote("aze");
		shiporder.withItem(2).setNote("qsd");
		shiporder.withItem(1).setNote("wxc");
		shiporder.withItem(3).setNote("rty");
		int i = 0;
		i++;
	}
}
