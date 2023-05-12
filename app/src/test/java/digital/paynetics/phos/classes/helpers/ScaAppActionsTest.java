package digital.paynetics.phos.classes.helpers;

import org.junit.Test;

import digital.paynetics.phos.sca.ScaAppActions;

import static org.junit.Assert.assertEquals;

public class ScaAppActionsTest {

    private String clientPackageName = "package.name.client";
    private final ScaAppActions sut = new ScaAppActions(clientPackageName);

    @Test
    public void getClientInitPinActionName() {
        assertEquals("package.name.client.INIT_PINPAD", sut.getClientInitPinActionName());
    }

    @Test
    public void getClientEnterPinActionName() {
        assertEquals("package.name.client.ENTER_PIN", sut.getClientEnterPinActionName());
    }

    @Test
    public void getInitPinAction_SHOULD_return_client_init_pin_action_WHEN_client_pin_app_is_available() {
        sut.setClientPinAppAvailable(true);

        assertEquals(sut.getClientInitPinActionName(), sut.getInitPinAction());
    }

    @Test
    public void getEnterPinAction_SHOULD_return_client_enter_pin_action_WHEN_client_pin_app_is_available() {
        sut.setClientPinAppAvailable(true);

        assertEquals(sut.getClientEnterPinActionName(), sut.getEnterPinAction());
    }

    @Test
    public void getInitPinAction_SHOULD_return_fallback_init_pin_action_WHEN_client_pin_app_is_available() {
        sut.setClientPinAppAvailable(false);

        assertEquals(sut.getFallbackInitPinActionName(), sut.getInitPinAction());
    }

    @Test
    public void getEnterPinAction_SHOULD_return_fallback_enter_pin_action_WHEN_client_pin_app_is_available() {
        sut.setClientPinAppAvailable(false);

        assertEquals(sut.getFallbackEnterPinActionName(), sut.getEnterPinAction());
    }
}