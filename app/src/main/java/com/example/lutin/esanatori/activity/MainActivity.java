package com.example.lutin.esanatori.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lutin.esanatori.EsanatoriApplication;
import com.example.lutin.esanatori.R;
import com.example.lutin.esanatori.dao.DefinitionDao;
import com.example.lutin.esanatori.dao.DefinitionDaoInterface;
import com.example.lutin.esanatori.dapter.AllWordAdapter;
import com.example.lutin.esanatori.dapter.DefinitionsAdapter;
import com.example.lutin.esanatori.model.RealmDefinition;
import com.example.lutin.esanatori.model.RealmDefinitions;
import com.example.lutin.esanatori.model.ResponseDefinitions;
import com.example.lutin.esanatori.service.WordsAPIService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.RealmList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {
    final static String regex = "[a-zA-Z]+";
    final static String APP_INFO = "eSanatori - Dinh Duc Thinh";
    final static List<String> ERRORS = Arrays.asList(
            "Oopps, you have not entered word yet, please enter any word",
            "Oopps, your input is not a word",
            "No definitions found with given word, try another one!",
            "Oopps, you have not selected any date!",
            "Your database is emty!"
    );
    int duration = Toast.LENGTH_LONG;
    private TextView allword;
    private TextView wordToday;
    private EditText word;
    private Button searchBtn;
    private Button randomBtn;
    private Button mywordsBtn;
    private Button deleteBtn;
    private Button deleteAllBtn;
    private CheckBox dateCB;
    private LinearLayout deleteLayout;
    private String KEYWORD = "bump";
    private String TAG = "WordsAPI";
    private WordsAPIService apiService;
    private Call<ResponseDefinitions> responseDefinitionsCall;
    private ResponseDefinitions responseDefinitions;
    private RealmDefinitions realmDefinitions;
    /*Variables for adding data to recyclerview*/
    private RecyclerView.LayoutManager layoutManager;
    private DefinitionDaoInterface dao = new DefinitionDao();
    private DefinitionsAdapter adapter;
    private AllWordAdapter allWordAdapter;
    private RecyclerView recyclerView;
    private RealmList<RealmDefinition> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        apiService = EsanatoriApplication.getInstance().getWordsAPIService();
        word = (EditText) findViewById(R.id.enterWordText);
        wordToday = (TextView) findViewById(R.id.wordTextView);
        wordToday.setVisibility(View.GONE);
        allword = (TextView) findViewById(R.id.allwordTextView);
        allword.setVisibility(View.GONE);
        searchBtn = (Button) findViewById(R.id.searchBtn);
        deleteBtn = (Button) findViewById(R.id.deleteBtn);
        deleteBtn.setVisibility(View.GONE);
        deleteLayout = (LinearLayout) findViewById(R.id.definitionLayout);
        //deleteLayout.setVisibility(View.GONE);
        deleteAllBtn = (Button) findViewById(R.id.deleteAllBtn);
        deleteAllBtn.setVisibility(View.GONE);


        // Search button click event handle
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                allword.setVisibility(View.GONE);
                deleteAllBtn.setVisibility(View.GONE);
                deleteBtn.setVisibility(View.GONE);
                String wordStr = word.getText().toString();
                Log.d(TAG, wordStr);
                if (wordStr.length() == 0) {
                    Context context = getApplicationContext();
                    Toast toast = Toast.makeText(context, ERRORS.get(0), duration);
                    toast.show();
                } else {
                    if (wordStr.matches(regex)) {
                        Log.d(TAG, "Connection successful " + dao.toString());
                        realmDefinitions = dao.isWordExist(wordStr);
                        if (realmDefinitions != null) {
                            Log.d(TAG, "Word is exist");
                            list = realmDefinitions.getRealmDefinitions();
                            Context context = getApplicationContext();
                            if (list.size() == 1) {
                                wordToday.setText("This word has " + list.size() + " definition");
                                wordToday.setVisibility(View.VISIBLE);
                                setupView(list, context);
                            } else {
                                wordToday.setText("This word has " + list.size() + " definitions");
                                wordToday.setVisibility(View.VISIBLE);
                                setupView(list, context);
                            }

                        } else {
                            Context context = getApplicationContext();
                            Log.d(TAG, "This word is not exsit in database");
                            fetchDefinitionThenStoreToRealm(wordStr, context);
                        }
                    } else {
                        Context context = getApplicationContext();
                        Toast toast = Toast.makeText(context, ERRORS.get(1), duration);
                        toast.show();
                    }

                }
            }
        });
        //Handle random word button click event
        randomBtn = (Button) findViewById(R.id.randomWordBtn);
        randomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                allword.setVisibility(View.GONE);
                deleteAllBtn.setVisibility(View.GONE);
                deleteBtn.setVisibility(View.GONE);
                word.setText("");
                int items = dao.findAllDefinitions().size();
                if (items == 0) {
                    Context context = getApplicationContext();
                    Toast toast = Toast.makeText(context, ERRORS.get(4), duration);
                    toast.show();
                } else {
                    int randomRow = dao.getRandomNumber(items);
                    Log.d("Items " + items, "Random row " + randomRow);
                    realmDefinitions = dao.getRandom(randomRow);
                    Context context = getApplicationContext();
                    wordToday.setText("Your random word is " + realmDefinitions.getWord().toUpperCase());
                    wordToday.setVisibility(View.VISIBLE);
                    setupView(realmDefinitions.getRealmDefinitions(), context);
                }

            }
        });
        // Handle my words button click event
        mywordsBtn = (Button) findViewById(R.id.myWordBtn);
        mywordsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wordToday.setVisibility(View.GONE);
                deleteAllBtn.setVisibility(View.VISIBLE);
                deleteBtn.setVisibility(View.VISIBLE);
                List<String> dateList = dao.getListDate();
                Log.d(TAG, "DATE is " + dateList.get(0));
                Context context = getApplicationContext();
                if (dateList.size() == 1) {
                    allword.setText("Your words has been collected in " + dateList.size() + " day");
                    allword.setVisibility(View.VISIBLE);
                    setupViewForMyWords(dateList, context);
                } else {
                    allword.setText("Your words has been collected in " + dateList.size() + " days");
                    allword.setVisibility(View.VISIBLE);
                    setupViewForMyWords(dateList, context);
                }
            }
        });

        // Handle delete button
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> dateList = dao.getListDate();
                List<String> selectedDate = getSelectedCheckbox(dateList);
                if (selectedDate.size() == 0) {
                    Context context = getApplicationContext();
                    Toast toast = Toast.makeText(context, ERRORS.get(3), duration);
                    toast.show();
                } else {
                    dao.deleteDenifitionsByDate(selectedDate);
                }
            }
        });

        // Hanlde delete all database button
        deleteAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dao.clearDatabase();
            }
        });

    }

    public List<String> getSelectedCheckbox(List<String> items) {
        dateCB = (CheckBox) findViewById(R.id.dateCheckBox);
        boolean selected = dateCB.isChecked();
        List<String> selectedList = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (selected) {
                String dateSelected = dateCB.getText().toString();
                selectedList.add(dateSelected);
            }
        }
        return selectedList;
    }

    @Override
    protected void onResume() {
        super.onResume();
        dao.open();
    }

    private void setupView(RealmList<RealmDefinition> list, Context context) {
        recyclerView = (RecyclerView) findViewById(R.id.definitionRecyclerView);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        adapter = new DefinitionsAdapter(list, context);
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "Show data to recyclerview");
    }

    private void setupViewForMyWords(List<String> list, Context context) {
        recyclerView = (RecyclerView) findViewById(R.id.definitionRecyclerView);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        allWordAdapter = new AllWordAdapter(list, context);
        recyclerView.setAdapter(allWordAdapter);
        Log.d(TAG, "Show all date to recyclerview");
    }


    private void fetchDefinitionThenStoreToRealm(final String wordStr, final Context context) {
        responseDefinitionsCall = apiService.getDefinitions(wordStr);
        responseDefinitionsCall.enqueue(new Callback<ResponseDefinitions>() {
            @Override
            public void onResponse(Call<ResponseDefinitions> call, Response<ResponseDefinitions> response) {
                if (response != null & response.isSuccessful()) {
                    responseDefinitions = response.body();
                    Log.d(TAG, "reponse message " + response.headers().hashCode());

                    RealmList<RealmDefinition> list = new RealmList<>();
                    for (int i = 0; i < responseDefinitions.getDefinitions().size(); i++) {
                        RealmDefinition realmDefinition = new RealmDefinition();
                        realmDefinition.setPartOfSpeech(responseDefinitions.getDefinitions().get(i).getPartOfSpeech());
                        realmDefinition.setDefinition(responseDefinitions.getDefinitions().get(i).getDefinition());
                        list.add(realmDefinition);
                    }
                    wordToday.setText("This word has " + list.size() + " definitions");
                    wordToday.setVisibility(View.VISIBLE);
                    setupView(list, context);
                    Log.d(TAG, "Definitions määrä " + responseDefinitions.getDefinitions().size());
                    dao.insertDefinitions(responseDefinitions);
                } else {
                    Toast toast = Toast.makeText(context, ERRORS.get(2), duration);
                    toast.show();
                }


            }

            @Override
            public void onFailure(Call<ResponseDefinitions> call, Throwable t) {
                Log.d("Failure ", "Cannot fetch data from Rest API " + t.getMessage());
            }
        });

    }


}

