package com.urish.adam.reddit;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SubredditPickerDialog.DialogListener, SwipeRefreshLayout.OnRefreshListener{
    public static RedditClient redditClient;
    public static final String POST_SETTING_NAME = "posts-load-amount";
    private String lastSubreddit;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        RedditManager redditManager = new RedditManager();
        redditManager.execute();
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this,SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if(id == R.id.subredditButton)
        {
            SubredditPickerDialog subredditPickerDialog = new SubredditPickerDialog();
            subredditPickerDialog.show(getFragmentManager(),"subredditPicker");
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    private void launchCustomTab(String url){
        CustomTabsIntent.Builder customTabBuilder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = customTabBuilder.build();
        customTabsIntent.launchUrl(this,Uri.parse(url));
    }
    private void setRedditClient(RedditClient redditClient){
        MainActivity.redditClient = redditClient;
        startSubredditUpdate(PreferenceManager.getDefaultSharedPreferences(this).getInt(POST_SETTING_NAME,20),null);
    }
    private void startSubredditUpdate(int amount, String subreddit){
        swipeRefreshLayout.setRefreshing(true);
        lastSubreddit = subreddit;
        SubredditGetter subredditGetter = new SubredditGetter(this);
        subredditGetter.setAmountToQuery(amount);
        subredditGetter.setSubToQuery(subreddit);
        subredditGetter.execute(MainActivity.redditClient);
        if(subreddit != null) {
            setTitle(subreddit);
        }
        else{
            setTitle("Frontpage");
        }
    }
    private void populateListView(Listing<Submission> submissions){
        Snackbar lowPostSnackbar = Snackbar.make(findViewById(R.id.drawer_layout),"There is nothing here",Snackbar.LENGTH_LONG);
        swipeRefreshLayout.setRefreshing(false);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        ArrayList<String> titles = new ArrayList<>();
        final Listing<Submission> finalSubmissions = submissions;
        for(Submission submission : submissions)
        {
            String titleBuilder = String.valueOf(submission.getScore()) +
                    " -- " +
                    submission.getTitle();
            titles.add(titleBuilder);
        }
        ListView listView = (ListView) findViewById(R.id.realTextField);
        ArrayAdapter<String> submissionArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles.toArray(new String[titles.size()]));
        AdapterView.OnItemLongClickListener itemLongClickListener = new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Submission clickedSubmission = finalSubmissions.get(i);
                getComments(clickedSubmission);
                return true;
            }
        };
        AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println("clicked");
                launchCustomTab(finalSubmissions.get(i).getUrl());
            }
        };
        listView.setAdapter(submissionArrayAdapter);
        listView.setOnItemClickListener(itemClickListener);
        listView.setOnItemLongClickListener(itemLongClickListener);
        ObjectAnimator listViewAnimator = ObjectAnimator.ofFloat(listView,"alpha",0f,1f);
        listViewAnimator.setDuration(250);
        listViewAnimator.start();
        if(submissions.size() < 2){
            Log.i("ListView","There are few posts, probably invalid subreddit");
            lowPostSnackbar.show();
        }
    }

    private void getComments(Submission submission) {
        Intent intent = new Intent(this,CommentActivity.class);
        intent.putExtra("SUBMISSION",submission.getId());
        startActivity(intent);
    }
    @Override
    public void onPosClick(DialogFragment dialogFragment) {
        ListView listView = (ListView) findViewById(R.id.realTextField);
        ObjectAnimator listViewAnimator = ObjectAnimator.ofFloat(listView,"alpha",1f,0f);
        listViewAnimator.setDuration(250);
        listViewAnimator.start();
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        startSubredditUpdate(PreferenceManager.getDefaultSharedPreferences(this).getInt(POST_SETTING_NAME,20),((EditText)dialogFragment.getDialog().findViewById(R.id.subredditPicker)).getText().toString());
    }

    @Override
    public void onRefresh() {
        startSubredditUpdate(PreferenceManager.getDefaultSharedPreferences(this).getInt(POST_SETTING_NAME,20),lastSubreddit);
    }

    private class SubredditGetter extends AsyncTask<RedditClient,Void,Listing<Submission>> {
        private int amountToQuery = 5;
        private String subToQuery;
        private Activity parentActivity;
        public void setAmountToQuery(int amountToQuery) {
            this.amountToQuery = amountToQuery;
        }
        public void setSubToQuery(String subToQuery) {
            this.subToQuery = subToQuery;
        }
        public SubredditGetter(Activity parentActivity){
            this.parentActivity = parentActivity;
        }
        @Override
        protected Listing<Submission> doInBackground(RedditClient... redditClients) {
            RedditClient redditClient = redditClients[0];
            SubredditPaginator subredditPaginator = new SubredditPaginator(redditClient);
            if(subToQuery != null){
                Log.i("SubredditGetter","Going to "+subToQuery);
                subredditPaginator.setSubreddit(subToQuery);
            }
            subredditPaginator.setLimit(amountToQuery);
            Listing<Submission> submissions;
            try {
                submissions = subredditPaginator.next();
            }
            catch (NetworkException e){
                Log.e("SubredditGetter", "Invalid subreddit, network error");
                Snackbar.make(this.parentActivity.findViewById(R.id.drawer_layout), "Invalid subreddit", Snackbar.LENGTH_LONG).show();
                submissions = new Listing<>(null);
            }
            return submissions;
        }

        @Override
        protected void onPostExecute(Listing<Submission> submissions) {
            super.onPostExecute(submissions);
            populateListView(submissions);
        }
    }
    private class RedditManager extends AsyncTask<Void,Void,RedditClient>{
        @Override
        protected RedditClient doInBackground(Void... voids) {
            UUID deviceUUID = UUID.randomUUID();
            UserAgent userAgent = UserAgent.of("android","com.urish.adam.reddit","v0.1","ack6600");
            System.out.println("generated useragent");
            RedditClient redditClient = new RedditClient(userAgent);
            System.out.println("generated redditclient");
            Credentials credentials = Credentials.userlessApp("R0Tl78hCI4Pq7A",deviceUUID);
            System.out.println("generated credentials");
            OAuthData oAuthData = null;
            try {
                oAuthData = redditClient.getOAuthHelper().easyAuth(credentials);
            } catch (NetworkException | OAuthException e) {
                e.printStackTrace();
            }
            if(oAuthData != null) {
                redditClient.authenticate(oAuthData);
                System.out.println("successfully authenticated");
            }
            else{
                Log.e("RedditManager","Authentication failed");
            }
            return redditClient;
        }

        @Override
        protected void onPostExecute(RedditClient redditClient) {
            super.onPostExecute(redditClient);
            setRedditClient(redditClient);
        }
    }
}
