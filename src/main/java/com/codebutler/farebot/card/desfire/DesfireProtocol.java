/*
 * DesfireProtocol.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.card.desfire;

import android.nfc.tech.IsoDep;
import android.util.Log;

import com.codebutler.farebot.util.Utils;

import java.io.ByteArrayOutputStream;

public class DesfireProtocol {
    /* Commands */
    static final byte GET_MANUFACTURING_DATA    = (byte) 0x60;
    static final byte GET_APPLICATION_DIRECTORY = (byte) 0x6A;
    static final byte GET_ADDITIONAL_FRAME      = (byte) 0xAF;
    static final byte SELECT_APPLICATION        = (byte) 0x5A;
    static final byte READ_DATA                 = (byte) 0xBD;
    static final byte READ_RECORD               = (byte) 0xBB;
    static final byte GET_FILES                 = (byte) 0x6F;
    static final byte GET_FILE_SETTINGS         = (byte) 0xF5;

    /* Status codes */
    static final byte OPERATION_OK      = (byte) 0x00;
    static final byte PERMISSION_DENIED = (byte) 0x9D;
    static final byte ADDITIONAL_FRAME  = (byte) 0xAF;

    private IsoDep mTagTech;

    private String humanCommand(byte command) {
        if(command == GET_MANUFACTURING_DATA) return "GET_MANUFACTURING_DATA";
        if(command == GET_APPLICATION_DIRECTORY) return "GET_APPLICATION_DIRECTORY";
        if(command == GET_ADDITIONAL_FRAME) return "GET_ADDITIONAL_FRAME";
        if(command == SELECT_APPLICATION) return "SELECT_APPLICATION";
        if(command == READ_DATA) return "READ_DATA";
        if(command == READ_RECORD) return "READ_RECORD";
        if(command == GET_FILES) return "GET_FILES";
        if(command == GET_FILE_SETTINGS) return "GET_FILE_SETTINGS";
        return "SOME OTHER COMMAND";
    }

    public DesfireProtocol(IsoDep tagTech) {
        mTagTech = tagTech;
    }

    public DesfireManufacturingData getManufacturingData() throws Exception {
        byte[] respBuffer = sendRequest(GET_MANUFACTURING_DATA);

        if (respBuffer.length != 28)
            throw new Exception("Invalid response");

        return new DesfireManufacturingData(respBuffer);
    }



    public int[] getAppList() throws Exception {
        byte[] appDirBuf = sendRequest(GET_APPLICATION_DIRECTORY);

        int[] appIds = new int[appDirBuf.length / 3];

        for (int app = 0; app < appDirBuf.length; app += 3) {
            byte[] appId = new byte[3];
            System.arraycopy(appDirBuf, app, appId, 0, 3);

            appIds[app / 3] = Utils.byteArrayToInt(appId);
        }

        return appIds;
    }

    public void selectApp(int appId) throws Exception {
        byte[] appIdBuff = new byte[3];
        appIdBuff[0] = (byte) ((appId & 0xFF0000) >> 16);
        appIdBuff[1] = (byte) ((appId & 0xFF00) >> 8);
        appIdBuff[2] = (byte) (appId & 0xFF);
        Log.i("selected application", readableByteArray(appIdBuff));
        sendRequest(SELECT_APPLICATION, appIdBuff);
    }

    public int[] getFileList() throws Exception {
        byte[] buf = sendRequest(GET_FILES);
        int[] fileIds = new int[buf.length];
        for (int x = 0; x < buf.length; x++) {
            fileIds[x] = (int)buf[x];
        }
        Log.i("file list", String.valueOf(fileIds.length));
        return fileIds;
    }

    public DesfireFileSettings getFileSettings(int fileNo) throws Exception {
        byte[] data = sendRequest(GET_FILE_SETTINGS, new byte[] { (byte) fileNo });
        return DesfireFileSettings.create(data);
    }

    public byte[] readFile(int fileNo) throws Exception {
        Log.i("fileNo", String.valueOf(fileNo));
        return sendRequest(READ_DATA, new byte[]{
                (byte) fileNo,
                (byte) 0x0, (byte) 0x0, (byte) 0x0,
                (byte) 0x0, (byte) 0x0, (byte) 0x0
        });
    }

    public byte[] readRecord(int fileNum) throws Exception {
        return sendRequest(READ_RECORD, new byte[]{
                (byte) fileNum,
                (byte) 0x0, (byte) 0x0, (byte) 0x0,
                (byte) 0x0, (byte) 0x0, (byte) 0x0
        });
    }

    private byte[] sendRequest(byte command) throws Exception {
        return sendRequest(command, null);
    }

    private byte[] sendRequest(byte command, byte[] parameters) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] recvBuffer = mTagTech.transceive(wrapMessage(command, parameters));
        if(command == READ_DATA) {
            Log.i("rec buffer", readableByteArray(recvBuffer));
            Log.i("output", readableByteArray(output.toByteArray()));
        }

        while (true) {
            if (recvBuffer[recvBuffer.length - 2] != (byte) 0x91)
                throw new Exception("Invalid response");

            output.write(recvBuffer, 0, recvBuffer.length - 2);

            byte status = recvBuffer[recvBuffer.length - 1];
            if (status == OPERATION_OK) {
                break;
            } else if (status == ADDITIONAL_FRAME) {
                recvBuffer = mTagTech.transceive(wrapMessage(GET_ADDITIONAL_FRAME, null));
            } else if (status == PERMISSION_DENIED) {
                throw new Exception("Permission denied");
            } else {
                Log.i("command", humanCommand(command));
                Log.i("status", String.valueOf(Integer.toHexString(status & 0xFF)));
                throw new Exception("Unknown status code: " + Integer.toHexString(status & 0xFF));
            }
        }

        return output.toByteArray();
    }

    private String readableByteArray(byte[] arr) {
        String message = "";
        for(int i=0; i<arr.length;i++) {
            byte b = arr[i];
            message += String.valueOf(Integer.toHexString(b & 0xFF))+" ";
        }
        return message;
    }

    private byte[] wrapMessage(byte command, byte[] parameters) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        stream.write((byte) 0x90);
        stream.write(command);
        stream.write((byte) 0x00);
        stream.write((byte) 0x00);
        if (parameters != null) {
            stream.write((byte) parameters.length);
            stream.write(parameters);
        }
        stream.write((byte) 0x00);

        if(command == READ_DATA) {
            Log.i("read data", readableByteArray(stream.toByteArray()));
        }
        if(command == GET_FILES) {
            Log.i("get files", readableByteArray(stream.toByteArray()));
        }

        return stream.toByteArray();
    }
}
