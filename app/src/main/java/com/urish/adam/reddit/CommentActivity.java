package com.urish.adam.reddit;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;

public class CommentActivity extends AppCompatActivity {
    RedditClient redditClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        Intent godIntent = getIntent();

        this.redditClient = MainActivity.redditClient;

        CommentRetriever commentRetriever = new CommentRetriever();
        commentRetriever.execute(godIntent.getStringExtra("SUBMISSION"));
    }
    public void showComments(CommentNode rootNode,String submissionTitle){
        setTitle(submissionTitle);
        ArrayList<String> commentList = new ArrayList<>();
        for(CommentNode child : rootNode){
            commentList.add(child.getComment().getBody());
        }
        ListView commentListView = (ListView) findViewById(R.id.commentListView);
        ArrayAdapter<String> commentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, commentList.toArray(new String[commentList.size()]));
        commentListView.setAdapter(commentAdapter);
        ObjectAnimator listViewAnimator = ObjectAnimator.ofFloat(commentListView,"alpha",0f,1f);
        listViewAnimator.setDuration(250);
        listViewAnimator.start();
    }
    private class CommentRetriever extends AsyncTask<String,Void,CommentNode> {
        Submission submissionToQuery;
        @Override
        protected CommentNode doInBackground(String... strings) {
            submissionToQuery = redditClient.getSubmission(strings[0]);
            return submissionToQuery.getComments();
        }
        @Override
        protected void onPostExecute(CommentNode commentNode) {
            super.onPostExecute(commentNode);
            showComments(commentNode,submissionToQuery.getTitle());
        }
    }
}

