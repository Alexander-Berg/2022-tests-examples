package com.yandex.maps.testapp.auth;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.List;

import com.yandex.passport.api.PassportAccount;
import com.yandex.maps.testapp.R;

public class AccountListAdapter extends ArrayAdapter<PassportAccount> {

    private static class ViewHolder {
        TextView type;
        TextView name;
        RadioButton selected;
    }

    public AccountListAdapter(Context context, List<PassportAccount> accounts, OnAccountClickListener onAccountClickedListener) {
        super(context, 0, accounts);
        onAccountClickedListener_ = onAccountClickedListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final PassportAccount account = getItem(position);

        final ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.auth_account, parent, false);

            viewHolder.type = convertView.findViewById(R.id.auth_account_type);
            viewHolder.name = convertView.findViewById(R.id.auth_account_name);
            viewHolder.selected = convertView.findViewById(R.id.auth_radio_selected);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.type.setText(account.getAndroidAccount().type);
        viewHolder.name.setText(styled(account.getAndroidAccount().name));

        final RadioButton selected = viewHolder.selected;
        selected.setChecked(account.isAuthorized() && account.equals(AuthUtil.getCurrentAccount()));

        selected.setOnClickListener(v -> onAccountClickedListener_.onAccountClicked(account));

        return convertView;
    }

    private Spannable styled(CharSequence string) {
        Spannable result = new SpannableString(string);
        result.setSpan(new ForegroundColorSpan(Color.RED), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return result;
    }

    private final OnAccountClickListener onAccountClickedListener_;
}
