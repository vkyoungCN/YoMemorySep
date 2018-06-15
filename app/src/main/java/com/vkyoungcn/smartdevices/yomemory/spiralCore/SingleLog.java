package com.vkyoungcn.smartdevices.yomemory.spiralCore;


import android.os.Parcel;
import android.os.Parcelable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("all")
/* 单条日志记录
* 日志记录数字从0始。0 是建组，1是初学，复习从2起。】
* 对应各组任务的一次学习和复习的记录
* 可以整理成简单的字串形式"N#yyyy-MM-dd HH:mm:ss#false;"【注意若使用DD会产生105这样的日期（实际应为15）】
* 记录内使用#分隔，每条记录以英文分号结尾。
*
* 注意，即时某次复习缺失，其记录次数也不应删除，否则影响计算。
* */
public class SingleLog implements Parcelable {
    private static final String TAG = "SingleLog";
    private short n=0;//本次记忆对应的次数；其中初次记忆=0，第n次复习= n。
    private long timeInMilli =0;//本次记忆对应的完成时间
    private boolean isMiss = false;//本次记忆是否未在规定时限内完成（未完成=miss=true）。

    public SingleLog() {
    }

    public SingleLog(String strLog) {
        if(strLog==null||strLog.isEmpty()){
            return;
        }
        String[] logSection = strLog.split("#");
        this.n = Short.valueOf(logSection[0]);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date  = simpleDateFormat.parse(logSection[1]);
            this.timeInMilli = date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        this.isMiss = logSection[2].equals("true");

    }

    public SingleLog(short n, long timeInMilli, boolean isMiss) {
        this.n = n;
        this.timeInMilli = timeInMilli;
        this.isMiss = isMiss;
    }

    public short getN() {
        return n;
    }

    public void setN(short n) {
        this.n = n;
    }

    public long getTimeInMilli() {
        return timeInMilli;
    }

    public void setTimeInMilli(long timeInMilli) {
        this.timeInMilli = timeInMilli;
    }

    public boolean isMiss() {
        return isMiss;
    }

    public void setMiss(boolean miss) {
        this.isMiss = miss;
    }

    public String toStrSingleLog(){
        StringBuilder sbd = new StringBuilder();
        sbd.append(this.n);
        sbd.append("#");

        SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdformat.format(this.timeInMilli);
        sbd.append(time);
        sbd.append("#");

        sbd.append(this.isMiss?"true":"false");
        sbd.append(";");

        return sbd.toString();
    }

    /*
    * 组建一条String格式的log记录。
    * 注意，生成的单条记录最后也要有分号！！
    * */
    public static String getStrSingleLogFromMillis(int num, long timeInMilli, Boolean isMiss){

        StringBuilder sbd = new StringBuilder();
        sbd.append(num);
        sbd.append("#");

        SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdformat.format(timeInMilli);
        sbd.append(time);
        sbd.append("#");

        sbd.append(isMiss?"true":"false");
        sbd.append(";");

        return sbd.toString();
    }



    /*
     * 以下是Parcelable要求的内容
     * */
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(n);
        parcel.writeLong(timeInMilli);
        parcel.writeByte((byte)( isMiss?1:0));
    }

    public static final Parcelable.Creator<SingleLog> CREATOR = new Parcelable.Creator<SingleLog>(){
        @Override
        public SingleLog createFromParcel(Parcel parcel) {
            return new SingleLog(parcel);
        }

        @Override
        public SingleLog[] newArray(int size) {
            return new SingleLog[size];
        }
    };

    private SingleLog(Parcel in){
        n = (short) in.readInt();
        timeInMilli = in.readLong();
        isMiss = in.readByte()==1;
    }

}
