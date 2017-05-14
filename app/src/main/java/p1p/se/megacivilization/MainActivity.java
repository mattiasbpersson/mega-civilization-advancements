package p1p.se.megacivilization;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
/*
 TODO:
 * Temporary bonus, like Library/Anatomy, is not handled
 * Killed app not reset correctly. Example: discount on next advancement
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ArrayList<AdvancementItem> list;
    private ListView listView;
    private AdvancementArrayAdaptor adapter;
    private TextView redCredit, greenCredit, blueCredit, yellowCredit, orangeCredit;
    private Preferences preferences;
    MenuItem buyMenu;
    private int advancementSelected = 0;
    private int totalPrice = 0;

    int blue, green, red, yellow, orange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        redCredit = (TextView) findViewById(R.id.red_label);
        greenCredit = (TextView) findViewById(R.id.green_label);
        blueCredit = (TextView) findViewById(R.id.blue_label);
        yellowCredit = (TextView) findViewById(R.id.yellow_label);
        orangeCredit = (TextView) findViewById(R.id.orange_label);

        preferences = new Preferences(this);

        list = new ArrayList<>();

        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                open(list.get(position));
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                AdvancementItem clickedItem = list.get(position);
                if (!clickedItem.owned) {
                    MainActivity.this.buyItems();
                }
                return true;
            }
        });


        try {
            list = readJson();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        adapter = new AdvancementArrayAdaptor(this, list);
        listView.setAdapter(adapter);
    }

    private void buyItems() {
        int bonus = 0;
        for (AdvancementItem item : list) {
            if (item.wantToBuy) {
                bonus += item.extraCredit;
                item.owned = true;
                adaptDiscount(item);
                MainActivity.this.addValues(item);
                item.wantToBuy = false;
            }
        }
        if (bonus > 0)
            selectBonus(bonus);
        else {
            upDateValues();
            resetBuyMenu();
        }

    }

    private void adaptDiscount(AdvancementItem item){
        if(item.next == null)
            return;
        for(AdvancementItem listItem : list){
            if(item.next.equals(listItem.name)){

                listItem.addDiscount();
                //Log.v(TAG, listItem.name + " price" + listItem.price());
                //return;
            }
        }
        Log.v(TAG, "ERROR discount missing for " + item.name);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions, menu);
        buyMenu = menu.findItem(R.id.action_buy);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reset_5:
                resetGame(10);
                return true;

            case R.id.action_reset_6:
                resetGame(5);
                return true;

            case R.id.action_reset:
                resetGame(0);
                return true;

            case R.id.action_buy:
                buyItems();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onPause() {
        storeData(getOwnedAdvancements());
        super.onPause();
    }

    @Override
    protected void onResume() {
        blue =  preferences.getCredit("blue");
        green =  preferences.getCredit("green");
        red =  preferences.getCredit("red");
        yellow =  preferences.getCredit("yellow");
        orange =  preferences.getCredit("orange");
        setOwnedAdvancements(preferences.getOwned());
        upDateValues();
        super.onResume();
    }

    private void resetGame(int defaultValue){
        blue = defaultValue;
        green = defaultValue;
        red = defaultValue;
        yellow = defaultValue;
        orange = defaultValue;
        storeData(new HashSet<String>());
        setOwnedAdvancements(preferences.getOwned());
        upDateValues();
    }

    private void storeData(HashSet<String> owned){
        preferences.storeCredit("blue", blue);
        preferences.storeCredit("green", green);
        preferences.storeCredit("red", red);
        preferences.storeCredit("yellow", yellow);
        preferences.storeCredit("orange", orange);
        preferences.storeOwned(owned);
    }

    private HashSet<String> getOwnedAdvancements() {
        HashSet<String> owned = new HashSet<>();
        for(AdvancementItem item : list){
            if(item.owned)
                owned.add(item.name);
        }
        return owned;
    }

    private void setOwnedAdvancements(Set<String> owned) {
        for(AdvancementItem item : list){
            if(owned.contains(item.name))
                item.owned = true;
            else
                item.owned = false;
        }
    }

    void upDateValues() {
        updateCreditValues();
        updateList();
        adapter.sort();
        adapter.notifyDataSetChanged();
    }

    public void open(final AdvancementItem item) {
        AdvancementItemDialog alertDialog = new AdvancementItemDialog(this,item);
        alertDialog.show();
    }


    public boolean selectBonus(final int bonus) {
        AdvancementBonusDialog bonusDialog = new AdvancementBonusDialog(this, bonus);
        return bonusDialog.show();
    }

    public ArrayList<AdvancementItem> readJson() throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(getAssets().open("data.json"), "UTF-8"));
        Log.v(TAG, "readJson");
        try {
            return readMessagesArray(reader);
        } finally {
            reader.close();
        }
    }

    public ArrayList<AdvancementItem> readMessagesArray(JsonReader reader) throws IOException {
        ArrayList<AdvancementItem> messages = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            messages.add(readMessage(reader));
        }
        reader.endArray();
        return messages;
    }

    public AdvancementItem readMessage(JsonReader reader) throws IOException {
        AdvancementItem adv = new AdvancementItem();

        reader.beginObject();
        while (reader.hasNext()) {
            String item = reader.nextName();
            if (item.equals("name")) {
                adv.name = reader.nextString();
            } else if (item.equals("color")) {
                adv.setColor(reader.nextString());
            } else if (item.equals("price")) {
                adv.setPrice(reader.nextInt());
            } else if (item.equals("red")) {
                adv.red = reader.nextInt();
            } else if (item.equals("green")) {
                adv.green = reader.nextInt();
            } else if (item.equals("yellow")) {
                adv.yellow = reader.nextInt();
            } else if (item.equals("orange")) {
                adv.orange = reader.nextInt();
            } else if (item.equals("blue")) {
                adv.blue = reader.nextInt();
            } else if (item.equals("next")) {
                adv.next = reader.nextString();
            } else if (item.equals("description")) {
                adv.description = reader.nextString();
            } else if (item.equals("special")) {
                adv.special = reader.nextString();
            } else if (item.equals("bonus")) {
                adv.bonus = reader.nextString();
            } else if (item.equals("extraCredit")) {
                adv.extraCredit = reader.nextInt();
            } else if (item.contains("calamity")) {
                adv.addCalamity(item, reader.nextString());
            } else
                reader.skipValue();
        }
        reader.endObject();
        return adv;
    }

    private void addValues(AdvancementItem item) {
        Log.v(TAG, "addValues:" + blue + ":" + red + ":" + yellow + ":" + orange + ":" + green);
        blue += item.blue;
        red += item.red;
        yellow += item.yellow;
        orange += item.orange;
        green += item.green;
        Log.v(TAG, "addValues:" + blue + ":" + red + ":" + yellow + ":" + orange + ":" + green);

    }

    private void removeValues(AdvancementItem item) {
        Log.v(TAG, "addValues:" + blue + ":" + red + ":" + yellow + ":" + orange + ":" + green);
        blue -= item.blue;
        red -= item.red;
        yellow -= item.yellow;
        orange -= item.orange;
        green -= item.green;
        Log.v(TAG, "addValues:" + blue + ":" + red + ":" + yellow + ":" + orange + ":" + green);
        updateCreditValues();
    }

    private void updateCreditValues() {
        blueCredit.setText(blue + "");
        redCredit.setText(red + "");
        orangeCredit.setText(orange + "");
        yellowCredit.setText(yellow + "");
        greenCredit.setText(green + "");
    }

    private void updateList() {
        for (AdvancementItem item : list) {
            item.updatePrice(blue, green, red, yellow, orange);
        }
    }

    public void calculatePrice(int price, boolean isChecked) {
        if(isChecked){
            totalPrice += price;
            advancementSelected++;
        }else{
            totalPrice -= price;
            advancementSelected--;
        }
        buyMenu.setEnabled(advancementSelected > 0);
        buyMenu.setTitle("Buy: " + totalPrice);
    }

    public void resetBuyMenu(){
        buyMenu.setEnabled(false);
        totalPrice = 0;
        advancementSelected = 0;
        buyMenu.setTitle("Buy: " + totalPrice);
    }
}

