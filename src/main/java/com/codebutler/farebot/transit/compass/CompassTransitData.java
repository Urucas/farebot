package com.codebutler.farebot.transit.compass;

import android.os.Parcel;
import android.util.Log;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.clipper.ClipperRefill;
import com.codebutler.farebot.transit.clipper.ClipperTrip;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

import java.util.List;

/**
 * Created by vruno on 11/9/15.
 */
public class CompassTransitData extends TransitData {

    private long            mSerialNumber;
    private short           mBalance;
    private ClipperTrip[]   mTrips;
    private ClipperRefill[] mRefills;

    public static boolean check(Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x10000) != null);
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getSerialNumber() {
        return null;
    }

    @Override
    public Trip[] getTrips() {
        return new Trip[0];
    }

    @Override
    public Refill[] getRefills() {
        return new Refill[0];
    }

    @Override
    public Subscription[] getSubscriptions() {
        return new Subscription[0];
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @Override
    public String getCardName() {
        return "Compass";
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    public CompassTransitData(Card card) {
        DesfireCard desfireCard = (DesfireCard) card;

        byte[] data;

        try {
            data = desfireCard.getApplication(0x10000).getFile(0x08).getData();
            mSerialNumber = Utils.byteArrayToLong(data, 1, 4);
            Log.i("serial number", String.valueOf(mSerialNumber));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Compass serial", ex);
        }

        try {
            data = desfireCard.getApplication(0x10000).getFile(0x02).getData();
            mBalance = (short) (((0xFF & data[18]) << 8) | (0xFF & data[19]));
            Log.i("serial number", String.valueOf(mBalance));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Compass balance", ex);
        }
    }
}
