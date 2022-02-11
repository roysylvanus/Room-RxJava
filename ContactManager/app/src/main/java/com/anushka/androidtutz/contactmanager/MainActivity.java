package com.anushka.androidtutz.contactmanager;

import android.arch.persistence.room.Room;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.anushka.androidtutz.contactmanager.adapter.ContactsAdapter;
import com.anushka.androidtutz.contactmanager.db.ContactsAppDatabase;
import com.anushka.androidtutz.contactmanager.db.entity.Contact;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private ContactsAdapter contactsAdapter;
    private ArrayList<Contact> contactArrayList = new ArrayList<>();
    private RecyclerView recyclerView;
    private ContactsAppDatabase contactsAppDatabase;
    private CompositeDisposable disposable=new CompositeDisposable();
    private long rowIdOfTheItemInserted;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(" Contacts Manager ");

        recyclerView = findViewById(R.id.recycler_view_contacts);
        contactsAppDatabase= Room.databaseBuilder(getApplicationContext(),ContactsAppDatabase.class,"ContactDB").build();

       // contactArrayList.addAll(contactsAppDatabase.getContactDAO().getContacts());

        contactsAdapter = new ContactsAdapter(this, contactArrayList, MainActivity.this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(contactsAdapter);

        disposable.add(contactsAppDatabase.getContactDAO().getContacts()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Contact>>() {
                               @Override
                               public void accept(List<Contact> contacts) throws Exception {

                                   contactArrayList.clear();
                                   contactArrayList.addAll(contacts);
                                   contactsAdapter.notifyDataSetChanged();
                               }
                           }, new Consumer<Throwable>() {
                               @Override
                               public void accept(Throwable throwable) throws Exception {


                               }
                           }
                )
        );

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAndEditContacts(false, null, -1);
            }


        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();


        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void addAndEditContacts(final boolean isUpdate, final Contact contact, final int position) {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        View view = layoutInflaterAndroid.inflate(R.layout.layout_add_contact, null);

        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilderUserInput.setView(view);

        TextView contactTitle = view.findViewById(R.id.new_contact_title);
        final EditText newContact = view.findViewById(R.id.name);
        final EditText contactEmail = view.findViewById(R.id.email);

        contactTitle.setText(!isUpdate ? "Add New Contact" : "Edit Contact");

        if (isUpdate && contact != null) {
            newContact.setText(contact.getName());
            contactEmail.setText(contact.getEmail());
        }

        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton(isUpdate ? "Update" : "Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {

                    }
                })
                .setNegativeButton("Delete",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {

                                if (isUpdate) {

                                    deleteContact(contact, position);
                                } else {

                                    dialogBox.cancel();

                                }

                            }
                        });


        final AlertDialog alertDialog = alertDialogBuilderUserInput.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(newContact.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Enter contact name!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    alertDialog.dismiss();
                }


                if (isUpdate && contact != null) {

                    updateContact(newContact.getText().toString(), contactEmail.getText().toString(), position);
                } else {

                    createContact(newContact.getText().toString(), contactEmail.getText().toString());
                }
            }
        });
    }

    private void deleteContact(final Contact contact, int position) {



        disposable.add( Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {

                contactsAppDatabase.getContactDAO().deleteContact(contact);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        Toast.makeText(getApplicationContext()," contact deleted successfully ", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(Throwable e) {

                        Toast.makeText(getApplicationContext()," error occurred ", Toast.LENGTH_LONG).show();

                    }
                }));
    }

    private void updateContact(final String name, final String email, int position) {

        final Contact contact = contactArrayList.get(position);

        contact.setName(name);
        contact.setEmail(email);



        disposable.add( Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {

                contactsAppDatabase.getContactDAO().updateContact(contact);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        Toast.makeText(getApplicationContext()," contact updated successfully ", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(Throwable e) {

                        Toast.makeText(getApplicationContext()," error occurred ", Toast.LENGTH_LONG).show();

                    }
                }));


    }

    private void createContact(final String name, final String email) {


       disposable.add( Completable.fromAction(new Action() {
           @Override
           public void run() throws Exception {

               rowIdOfTheItemInserted = contactsAppDatabase.getContactDAO().addContact(new Contact(0,name, email));
           }
       })
               .subscribeOn(Schedulers.io())
               .observeOn(AndroidSchedulers.mainThread())
               .subscribeWith(new DisposableCompletableObserver() {
                   @Override
                   public void onComplete() {
                       Toast.makeText(getApplicationContext()," contact added successfully "+rowIdOfTheItemInserted, Toast.LENGTH_LONG).show();
                   }

                   @Override
                   public void onError(Throwable e) {

                       Toast.makeText(getApplicationContext()," error occurred ", Toast.LENGTH_LONG).show();

                   }
               }));



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disposable.dispose();
    }
}
