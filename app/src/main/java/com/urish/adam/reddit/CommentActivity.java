package com.urish.adam.reddit;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.collect.FluentIterable;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class CommentActivity extends AppCompatActivity {
    public static final int MARGIN_INCREMENT = 5;
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
        ArrayList<CommentNode> commentList = new ArrayList<>();
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.commentRelativeLayout);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        for(CommentNode child : rootNode){
            commentList.add(child);
        }
        for(CommentNode commentNode : commentList){
            FluentIterable<CommentNode> iterable = commentNode.walkTree();
            TextView lastView = null;
            for(CommentNode commentNode1 : iterable){
                TextView textView = new TextView(this);
                textView.setText(commentNode1.getComment().getBody());
                layoutParams.setMargins(commentNode1.getDepth()*MARGIN_INCREMENT,0,0,0);
                if(lastView != null) {
                    layoutParams.addRule(RelativeLayout.BELOW, lastView.getId());
                }
                textView.setLayoutParams(layoutParams);
                relativeLayout.addView(textView);
                lastView = textView;
            }
        }
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

