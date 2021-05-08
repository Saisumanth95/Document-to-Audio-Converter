package com.saisumanth.documenttoaudioconverter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class Adapter extends RecyclerView.Adapter<Adapter.MyViewHolder> {

    private ArrayList<Item> list;
    private Context context;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;


    public interface OnItemClickListener{

        void onItemClick(int position);

    }

    public interface OnItemLongClickListener{

        void onItemLongClick(int position);

    }

    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener){
        this.longClickListener = longClickListener;
    }


    public static class MyViewHolder extends RecyclerView.ViewHolder{

        public TextView fileTitle;
        public TextView created;

        public MyViewHolder(@NonNull View itemView, final OnItemClickListener listener,final OnItemLongClickListener longClickListener) {
            super(itemView);

            fileTitle = itemView.findViewById(R.id.item_title);
            created = itemView.findViewById(R.id.item_created);


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(listener != null){

                        int position = getAdapterPosition();

                        if(position != RecyclerView.NO_POSITION){
                            listener.onItemClick(position);
                        }

                    }

                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    if(longClickListener != null){
                        int position = getAdapterPosition();

                        if(position != RecyclerView.NO_POSITION) {
                            longClickListener.onItemLongClick(position);
                        }


                    }

                    return true;
                }
            });


        }

    }


    public Adapter(ArrayList<Item> list, Context context){

        this.list = list;
        this.context = context;

    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item,parent,false);

        MyViewHolder mv = new MyViewHolder(v,listener,longClickListener);

        return mv;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        final Item currentItem =list.get(position);

        holder.fileTitle.setText(currentItem.getFilename());

        CharSequence dateChar = DateFormat.format("EEEE, MMM d,yyyy h:mm a",currentItem.getTime().toDate());

        holder.created.setText(dateChar);

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

}
