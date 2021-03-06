package com.pd.nextmovie.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.algolia.instantsearch.ui.utils.ItemClickSupport;
import com.algolia.instantsearch.ui.views.Hits;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pd.chocobar.ChocoBar;
import com.pd.nextmovie.R;
import com.pd.nextmovie.activities.MoviesActivity;
import com.pd.nextmovie.asynctask.GetImageFromURI;
import com.pd.nextmovie.model.Bookmarks;
import com.pd.nextmovie.model.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class MoviesFragment extends MoviesActivity.MovieTabActivity.LayoutFragment {

    private Bookmarks bookmarks;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public MoviesFragment() {
        super(R.layout.fragment_movies);
        bookmarks = new Bookmarks();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Hits hits = view.findViewById(R.id.hits_movies);

        // recommendation system without using * actual * machine learning, trying to figure out how to do so
        // algorithm:
        // 1. go through the bookmarks and find genres that are common and store the common genre list
        // 2. find movies with genre list much similar to the common list of the user to recommend movies
        // 3. remove those movies that are already in the bookmark list

        final Set<String> commonGenres = new HashSet<>(); // currently using set, but employ hashMap to find genres that are more common
        assert FirebaseAuth.getInstance().getUid() != null;
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getUid());

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for(DataSnapshot ds : dataSnapshot.child("bookmarks").getChildren()){
                    for(DataSnapshot genreDS : ds.child("genre").getChildren()){
                        String genre = (String) genreDS.getValue();
                        commonGenres.add(genre);
                    }
                }

                List<String> common = new ArrayList<>(commonGenres);
                ref.child("liking").setValue(common);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("DatabaseError", databaseError.toString());
            }
        });

        hits.setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, int position, View v) {
                JSONObject jsonObject = hits.get(position);
                String title;
                String image;
                String genre;
                int year;
                int rating;
                double score;

                try {
                    title = jsonObject.getString("title");
                    image = jsonObject.getString("image");

                    JSONArray jsonArray = jsonObject.getJSONArray("genre");
                    StringBuilder sb = new StringBuilder();
                    for(int i=0;i<jsonArray.length();i++){
                        if(i == jsonArray.length() - 1)
                            sb.append(jsonArray.get(i)).append(".");
                        else
                            sb.append(jsonArray.get(i)).append(", ");
                    }

                    genre = sb.toString();

                    year = jsonObject.getInt("year");
                    rating = jsonObject.getInt("rating");
                    score = jsonObject.getDouble("score");
                    Double truncatedDouble = BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP).doubleValue();

                    Drawable drawable = new GetImageFromURI().execute(image).get();

                    if(!genre.contains(",")){
                        MaterialStyledDialog dialog = new MaterialStyledDialog.Builder(MoviesFragment.this.getContext())
                                .setTitle(title)
                                .setIcon(drawable)
                                .setDescription("Released in "+year+", having MovieBook score of "+rating+" and average rating of "
                                        +truncatedDouble+" falling in category of "+genre)
                                .setPositiveText("OK")
                                .build();

                        dialog.show();
                    }
                    else {
                        MaterialStyledDialog dialog = new MaterialStyledDialog.Builder(MoviesFragment.this.getContext())
                                .setTitle(title)
                                .setIcon(drawable)
                                .setDescription("Released in " + year + ", having MovieBook score of " + rating + " and average rating of "
                                        + truncatedDouble + " falling in categories " + genre)
                                .setPositiveText("OK")
                                .build();

                        dialog.show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                //Log.d("Clicked_object: ",jsonObject.toString());

                // use a custom action box to show more details about the movie



                try {
                    Log.d("Clicked","Clicked on the hit "+jsonObject.getString("title"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        hits.setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView recyclerView, int position, View v) {
                JSONObject jsonObject = hits.get(position);

                try {

                    if(FirebaseAuth.getInstance().getUid() == null){
                        ChocoBar.builder().setActivity(MoviesFragment.this.getActivity())
                                .setText("Please login first by clicking on the bookmark button")
                                .setDuration(ChocoBar.LENGTH_SHORT)
                                .orange()
                                .show();
                    }
                    else {
                        final String movieTitle = jsonObject.getString("title");
                        final String image = jsonObject.getString("image");
                        final int rating = jsonObject.getInt("rating");
                        final int year = jsonObject.getInt("year");
                        final JSONArray genre = jsonObject.getJSONArray("genre");

                        //Log.d("genre: ",genre.toString());

                        Drawable drawable = new GetImageFromURI().execute(image).get();

                        ChocoBar.builder().setBackgroundColor(Color.parseColor("#000000"))
                                .setTextSize(15)
                                .setTextColor(Color.parseColor("#FFFFFF"))
                                .setTextTypefaceStyle(Typeface.ITALIC)
                                .setText("Bookmarked " + movieTitle)
                                .setMaxLines(5)
                                .centerText()
                                .setActionText("OK")
                                .setActionClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Movie movie = new Movie(movieTitle, image);
                                        ArrayList<String> movieGenre = new ArrayList<>();

                                        for(int i=0;i<genre.length();i++){
                                            try {
                                                movieGenre.add(genre.getString(i));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        movie.setRating(rating);
                                        movie.setYear(year);
                                        movie.setGenre(movieGenre);

                                        bookmarks.addMovie(movie);
                                        bookmarks.addBookmarksToDatabase();
                                    }
                                })
                                .setActionTextColor(Color.parseColor("#66FFFFFF"))
                                .setActionTextSize(20)
                                .setIcon(drawable)
                                .setActivity(MoviesFragment.this.getActivity())
                                .setDuration(ChocoBar.LENGTH_INDEFINITE)
                                .build()
                                .show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                return true;
            }
        });
    }

}