package network;

import utils.Log;
import utils.LogLevel;
import utils.Util;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class FileSender implements Runnable{

    private final String TAG = FileSender.class.getSimpleName();
    private FileSenderCallback callback;

    private final int BLOCK_SIZE = 1024;
    private Socket socket;
    private InetAddress targetAddr;
    private int targetPort;
    private File file;

    public FileSender(InetAddress targetAddr, int targetPort, File file, FileSenderCallback callback){
        this.targetAddr = targetAddr;
        this.targetPort = targetPort;
        this.file = file;
        this.callback = callback;
    }

    @Override
    public void run() {
        try{
            socket = new Socket(targetAddr, targetPort);
            sendFileBySocket(file, socket.getOutputStream());
            if(null != callback){
                callback.finish();
            }

        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try{
                if(socket != null){
                    socket.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }


    }

    public void sendFileBySocket(File file, OutputStream os)
    {
        String fileName = file.getName();
        byte[] fileNameByte = fileName.getBytes();
        int fileNameLen = fileNameByte.length;
        long fileLen = file.length();
        if(fileLen > Integer.MAX_VALUE)
        {
            Log.log(TAG, LogLevel.ERROR, "file size bigger than the max value of Long! ");
            return;
        }
        try {
            os.write(Util.int2ByteArrays(fileNameLen));
            Log.log(TAG, LogLevel.INFO, "write fileNameLen:"+fileNameLen);
            os.write(fileNameByte);
            Log.log(TAG, LogLevel.INFO, "write fileNameByte");
            os.write(Util.long2ByteArrays(fileLen));
            Log.log(TAG, LogLevel.INFO, "write fileLen:"+fileLen);
            writeFileToOs(file, os);
            os.flush();
            Log.log(TAG, LogLevel.INFO, "send file finished!");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void writeFileToOs(File file, OutputStream os) throws IOException
    {
        long fileTotalSize = file.length();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        DataOutputStream bos = new DataOutputStream(os);
        byte[] buffer = new byte[BLOCK_SIZE];
        int once_read_len = 0;
        long totalSizeHaveSent = 0;
        while((once_read_len = bis.read(buffer)) != -1)
        {
            bos.write(buffer, 0, once_read_len);
            totalSizeHaveSent += once_read_len;
            if(callback != null)
            {
                callback.currentProgress((int)(totalSizeHaveSent*100/fileTotalSize));
            }

        }
        os.flush();
        Log.log(TAG, LogLevel.INFO, "total send:"+totalSizeHaveSent);
    }
}
