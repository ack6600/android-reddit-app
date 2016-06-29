package com.urish.adam.reddit;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.collect.FluentIterable;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import in.uncod.android.bypass.Bypass;

public class CommentActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{
    public static final int MARGIN_INCREMENT = 5;
    RedditClient redditClient;
    SwipeRefreshLayout swipeRefreshLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        Intent godIntent = getIntent();

        this.redditClient = MainActivity.redditClient;

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.commentSwipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);

        CommentRetriever commentRetriever = new CommentRetriever();
        commentRetriever.execute(godIntent.getStringExtra("SUBMISSION"));
    }
    public void showComments(final CommentNode rootNode, final String submissionTitle){
        setTitle(submissionTitle);
        ListView listView = (ListView) findViewById(R.id.commentListView);
        ArrayList<CommentNode> commentNodeList = new ArrayList<>();
        if(!rootNode.isEmpty()) {
            for (CommentNode commentNode : rootNode) {
                commentNodeList.add(commentNode);
            }
            final ArrayList<CommentNode> finalList = commentNodeList;
            listView.setAdapter(new ArrayAdapter<CharSequence>(this, android.R.layout.simple_list_item_1, commentNodeListToArray(finalList)));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    CommentActivity.this.showComments(finalList.get(i), submissionTitle);
                }
            });
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (!finalList.get(i).isTopLevel()) {
                        CommentActivity.this.showComments(rootNode.getParent(), submissionTitle);
                    }
                    return true;
                }
            });
            ObjectAnimator listViewAnimator = ObjectAnimator.ofFloat(listView, "alpha", 0f, 1f);
            listViewAnimator.setDuration(250);
            listViewAnimator.start();
            swipeRefreshLayout.setRefreshing(false);
            if (finalList.size() < 1) {
                Snackbar.make(findViewById(R.id.commentRelativeLayout), "Comment has no children", Snackbar.LENGTH_LONG).show();
                showComments(rootNode.getParent(), submissionTitle);
            }
            Snackbar.make(findViewById(R.id.commentRelativeLayout), "No comments", Snackbar.LENGTH_LONG).show();
        }
    }
    private CharSequence[] commentNodeListToArray(ArrayList<CommentNode> commentNodes){
        Bypass bypass = new Bypass(this);
        ArrayList<CharSequence> commentBodies = new ArrayList<>();
        for( CommentNode commentNode : commentNodes ){
            commentBodies.add(bypass.markdownToSpannable(commentNode.getComment().getBody()).toString());
        }
        return commentBodies.toArray(new CharSequence[commentBodies.size()]);
    }

    @Override
    public void onRefresh() {
        CommentRetriever commentRetriever = new CommentRetriever();
        commentRetriever.execute(getIntent().getStringExtra("SUBMISSION"));
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

