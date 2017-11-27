package com.kedacom.vconfsdk.persistence;

/**
 * Created by Sissi on 11/3/2017.
 */
public class AccessListeners {

    public interface OnPutFinishedListener {
        void onPutSuccess();
        void onPutFailed();
    }

    public interface OnGetFinishedListener{
        void onGetSuccess(Object obj);
        void onGetFailed();
    }

    public interface OnDelFinishedListener{
        void onDelFinished();
    }
}
