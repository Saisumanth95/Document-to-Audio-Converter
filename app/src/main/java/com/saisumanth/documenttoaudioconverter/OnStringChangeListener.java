package com.saisumanth.documenttoaudioconverter;

public interface OnStringChangeListener {

    public void onStringChanged(String newValue);

}

class ObserveString{

    private OnStringChangeListener listener;

    private String value;

    public void setOnStringChangeListener(OnStringChangeListener listener){

        this.listener = listener;

    }

    public String get(){
        return value;
    }

    public void set(String value){

        this.value = value;

        if(listener != null){
            listener.onStringChanged(value);
        }

    }

}