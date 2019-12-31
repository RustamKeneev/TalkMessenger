package com.example.talkmessenger;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessagesViewHolder>{

    private List<Message> messages;
    private Context context;
    private static final int MY_TYPE_MESSAGE = 0;
    private static final int TYPE_OTHER_MESSAGE = 1;

    public MessagesAdapter(Context context) {
        messages = new ArrayList<>();
        this.context = context;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == MY_TYPE_MESSAGE){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_message,parent,false);
        }else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message,parent,false);
        }
        return new MessagesViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        String author = message.getAuthor();
        if (author !=null && author.equals(PreferenceManager.getDefaultSharedPreferences(context).getString("author","anonim"))){
            return MY_TYPE_MESSAGE;
        }else {
            return TYPE_OTHER_MESSAGE;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessagesViewHolder holder, int position) {
        Message message = messages.get(position);
        String author = message.getAuthor();
        String textOfMessage = message.getTextOfMessage();
        String urlToUmage = message.getImageUrl();
        holder.text_view_author.setText(author);
        if (textOfMessage !=null && !textOfMessage.isEmpty()){
            holder.text_view_message.setText(textOfMessage);
            holder.imageView_in_storage.setVisibility(View.GONE);
        }else {
            holder.imageView_in_storage.setVisibility(View.VISIBLE);
        }
        holder.text_view_author.setText(author);
        if (urlToUmage != null && !urlToUmage.isEmpty()){
            holder.text_view_message.setVisibility(View.VISIBLE);
            holder.text_view_message.setText(textOfMessage);
        }else {
            holder.text_view_message.setVisibility(View.GONE);
        }
        if (urlToUmage !=null && !urlToUmage.isEmpty()){
            Picasso.get().load(urlToUmage).into(holder.imageView_in_storage);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class MessagesViewHolder extends RecyclerView.ViewHolder{

        TextView text_view_author;
        TextView text_view_message;
        ImageView imageView_in_storage;

        public MessagesViewHolder(@NonNull View itemView) {
            super(itemView);
            text_view_author = itemView.findViewById(R.id.text_view_author);
            text_view_message = itemView.findViewById(R.id.text_view_message);
            imageView_in_storage = itemView.findViewById(R.id.imageView_in_storage);
        }
    }
}
