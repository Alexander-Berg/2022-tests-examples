package com.yandex.maps.testapp.auth;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.yandex.maps.auth.internal.DropTokenTask;
import com.yandex.maps.auth.internal.GetAccountResultReceiver;
import com.yandex.maps.auth.internal.GetAccountTask;
import com.yandex.maps.auth.internal.GetPassportAccountsTask;
import com.yandex.maps.auth.internal.GetTokenTask;
import com.yandex.maps.auth.internal.LogoutTask;
import com.yandex.passport.api.Passport;
import com.yandex.passport.api.PassportAccount;
import com.yandex.passport.api.PassportLoginProperties;
import com.yandex.passport.api.PassportLoginResult;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.passport.api.PassportToken;
import com.yandex.passport.api.PassportUid;
import com.yandex.passport.api.exception.PassportAccountNotAuthorizedException;
import com.yandex.passport.api.exception.PassportAccountNotFoundException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AuthActivity extends TestAppActivity implements OnAccountClickListener, GetAccountResultReceiver {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        accountListAdapter_ = new AccountListAdapter(
            this,
            new ArrayList<>(),
            this);

        fullDataReload();

        ListView listView = findViewById(R.id.auth_account_list);
        listView.setAdapter(accountListAdapter_);
        listView.setEmptyView(findViewById(R.id.auth_empty_list));

        registerForContextMenu(listView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // To ensure removed accounts removed.
        fullDataReload();
    }

    @Override
    protected void onStartImpl(){}
    @Override
    protected void onStopImpl(){}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_auth, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemid = item.getItemId();
        if (itemid == R.id.auth_settings_action_add) {
            addAccount();
            return true;
        } else if (itemid == R.id.auth_settings_action_drop_token) {
            PassportToken currentAccountToken = AuthUtil.getCurrentAccountToken();
            if (currentAccountToken != null) {
                final DropTokenTask task = new DropTokenTask(this, AuthUtil.passportApi_);
                task.execute(currentAccountToken.getValue());
            }
            return true;
        } else if (itemid == R.id.auth_settings_action_logout) {
            logout();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAccountClicked(final PassportAccount account) {
        if (account.isAuthorized() && AuthUtil.hasToken(account)) {
            AuthUtil.setCurrentAccount(account);
            accountListAdapter_.notifyDataSetChanged();
            return;
        }

        if (dialog_ == null) {
            dialog_ = new ProgressDialog.Builder(this)
                .setTitle("Validating token...")
                .create();
        }

        final GetTokenTask task = new GetTokenTask(this, AuthUtil.passportApi_);
        task.execute(account);
        dialog_.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Log.d("AuthActivity", "RESULT_OK");
            if (requestCode == AuthUtil.GET_ACCOUNTS) {
                Log.d("AuthActivity", "GET_ACCOUNTS");
            } else if (requestCode == AuthUtil.REQUEST_CODE_RELOGIN) {
                Log.d("AuthActivity", "REQUEST_CODE_RELOGIN");
            }
            final PassportLoginResult passportLoginResult = Passport.createPassportLoginResult(data);
            final PassportUid passportUid = passportLoginResult.getUid();

            final GetAccountTask task = new GetAccountTask(this, AuthUtil.passportApi_);
            task.execute(passportUid);
        }
    }

    private void addAccount() {
        final PassportLoginProperties loginProperties = AuthUtil.createPassportLoginProperties();
        Intent intent = AuthUtil.passportApi_.createLoginIntent(this, loginProperties);
        startActivityForResult(intent, AuthUtil.GET_ACCOUNTS);
    }

    private void logout() {
        PassportAccount account = AuthUtil.getCurrentAccount();
        if (account == null)
            return;

        final LogoutTask task = new LogoutTask(this, AuthUtil.passportApi_);
        task.execute(account.getUid());
    }

    private void fullDataReload() {
        final GetPassportAccountsTask task = new GetPassportAccountsTask(this, AuthUtil.passportApi_);
        task.execute(AuthUtil.defaultFilter);
    }

    @Override
    public void onGetAccountResultReceived(@NotNull PassportAccount passportAccount) {
        Log.d("AuthResultReceiver","Get account uid " + passportAccount.getUid().getValue());

        if (accountListAdapter_.getPosition(passportAccount) == -1)
            accountListAdapter_.add(passportAccount);

        SharedPreferences ref = getApplicationContext().getSharedPreferences("YANDEX_ACCOUNTS", 0);
        Set<String> accounts = ref.getStringSet("accounts", new HashSet<String>());
        accounts.add(String.valueOf(passportAccount.getUid().getValue()));
        SharedPreferences.Editor editor = ref.edit();
        // See bug https://issuetracker.google.com/issues/36943216
        editor.remove("accounts");
        editor.apply();
        editor.putStringSet("accounts", accounts);
        editor.apply();

        onAccountClicked(passportAccount);
    }

    @Override
    public void onGetTokenResultReceived(@NonNull PassportAccount account, @NotNull PassportToken passportToken) {
        Log.d("AuthResultReceiver", "Get token for account: " + account.getAndroidAccount().name);
        AuthUtil.setToken(account, passportToken);
        accountListAdapter_.notifyDataSetChanged();
        hideDialog();
    }

    @Override
    public void onGetPassportAccountsResultReceived(@NotNull List<PassportAccount> accounts) {
        Log.d("AuthActivity", "Get accounts size = " + accounts.size());
        accountListAdapter_.clear();

        SharedPreferences ref = getApplicationContext().getSharedPreferences("YANDEX_ACCOUNTS", 0);
        Set<String> saved_accounts = ref.getStringSet("accounts", new HashSet<String>());

        if (saved_accounts != null) {
            Iterator<PassportAccount> accountIterator = accounts.iterator();
            while (accountIterator.hasNext()) {
                PassportAccount account = accountIterator.next();
                if (!saved_accounts.contains(String.valueOf(account.getUid().getValue())))
                    accountIterator.remove();
            }
        } else {
            accounts.clear();
        }

        accountListAdapter_.addAll(accounts);
        accountListAdapter_.notifyDataSetChanged();
    }

    @Override
    public void onSuccessDropToken(@NonNull String token) {
        AuthUtil.removeToken(token);
        accountListAdapter_.notifyDataSetChanged();
    }

    @Override
    public void onPassportApiErrorReceived(@NonNull Throwable ex) {
        accountListAdapter_.notifyDataSetChanged();
        Toast.makeText(this, "Get passport error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();

        if (ex instanceof PassportAccountNotAuthorizedException || ex instanceof PassportAccountNotFoundException) {
            final PassportLoginProperties loginProperties = AuthUtil.createPassportLoginProperties();
            Intent intent = AuthUtil.passportApi_.createLoginIntent(this, loginProperties);
            startActivityForResult(intent, AuthUtil.REQUEST_CODE_RELOGIN);
        }
        cancelDialog();
    }

    @Override
    public void onSuccessLogout(@NonNull PassportUid uid) {
        AuthUtil.removeAccount(uid);

        for (int i = 0; i < accountListAdapter_.getCount(); ++i) {
            PassportAccount account = accountListAdapter_.getItem(i);
            if (account.getUid() == uid) {
                accountListAdapter_.remove(account);
                break;
            }
        }

        accountListAdapter_.notifyDataSetChanged();

        SharedPreferences ref = getApplicationContext().getSharedPreferences("YANDEX_ACCOUNTS", 0);
        Set<String> accounts = ref.getStringSet("accounts", new HashSet<String>());
        accounts.remove(String.valueOf(uid.getValue()));
        SharedPreferences.Editor editor = ref.edit();
        // See bug https://issuetracker.google.com/issues/36943216
        editor.remove("accounts");
        editor.apply();
        editor.putStringSet("accounts", accounts);
        editor.apply();
    }

    private void hideDialog() {
        if (dialog_ != null) {
            dialog_.hide();
        }
    }

    private void cancelDialog() {
        if (dialog_ != null) {
            dialog_.cancel();
        }
    }

    private AccountListAdapter accountListAdapter_;
    private Dialog dialog_ = null;
}
