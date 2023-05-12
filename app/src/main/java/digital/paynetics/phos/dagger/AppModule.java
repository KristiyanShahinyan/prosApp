package digital.paynetics.phos.dagger;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationManager;
import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;

import dagger.Module;
import dagger.Provides;
import digital.paynetics.phos.app2app.OperationRequestParser;
import digital.paynetics.phos.app2app.OperationRequestParserImpl;
import digital.paynetics.phos.app2app.OperationRequestTypeAdapterFactory;
import digital.paynetics.phos.app2app.PayloadSigner;
import digital.paynetics.phos.app2app.PayloadSignerNoSigning;
import digital.paynetics.phos.app2app.TransactionDateTypeAdapter;

@Module
public class AppModule {

    public static AppModule create() {
        return new AppModule();
    }

    @Provides
    @PhosScope
    NotificationManager provideNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }

    @Provides
    @PhosScope
    Gson provideOperationsGson() {
        GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapterFactory(new OperationRequestTypeAdapterFactory())
                .registerTypeAdapter(Date.class, new TransactionDateTypeAdapter());
        return builder.create();
    }


    @Provides
    @PhosScope
    PayloadSigner providePayloadSigner() {
        return new PayloadSignerNoSigning();
    }

    @Provides
    @PhosScope
    OperationRequestParser provideOperationRequestParser(Gson gson) {
        return new OperationRequestParserImpl(gson);
    }

}
