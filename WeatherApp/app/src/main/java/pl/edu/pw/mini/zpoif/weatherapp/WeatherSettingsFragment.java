package pl.edu.pw.mini.zpoif.weatherapp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.HashMap;
import java.util.Map;

public class WeatherSettingsFragment extends Fragment {

    private OnWeatherSettingsListener listener;
    private EditText etDays;
    private Button btnApply;


    private Map<String, CheckBox> checkBoxes = new HashMap<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnWeatherSettingsListener) {
            listener = (OnWeatherSettingsListener) context;
        } else {
            throw new RuntimeException("Activity must implement OnWeatherSettingsListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather_settings, container, false);

        etDays = view.findViewById(R.id.et_days);
        btnApply = view.findViewById(R.id.btn_apply);


        checkBoxes.put("temp", view.findViewById(R.id.cb_temp));
        checkBoxes.put("humidity", view.findViewById(R.id.cb_humidity));
        checkBoxes.put("apparent", view.findViewById(R.id.cb_apparent));
        checkBoxes.put("precip_prob", view.findViewById(R.id.cb_precip_prob));
        checkBoxes.put("precip", view.findViewById(R.id.cb_precip));
        checkBoxes.put("rain", view.findViewById(R.id.cb_rain));
        checkBoxes.put("snow", view.findViewById(R.id.cb_snow));
        checkBoxes.put("pressure", view.findViewById(R.id.cb_pressure));
        checkBoxes.put("cloud", view.findViewById(R.id.cb_cloud));
        checkBoxes.put("visibility", view.findViewById(R.id.cb_visibility));
        checkBoxes.put("wind", view.findViewById(R.id.cb_wind));

        btnApply.setOnClickListener(v -> {
            String daysStr = etDays.getText().toString();
            if(daysStr.isEmpty()) return;

            int days = Integer.parseInt(daysStr);
            if(days < 1) days = 1;
            if(days > 16) days = 16;


            Map<String, Boolean> selectedOptions = new HashMap<>();
            for (Map.Entry<String, CheckBox> entry : checkBoxes.entrySet()) {
                selectedOptions.put(entry.getKey(), entry.getValue().isChecked());
            }


            listener.onSettingsChanged(days, selectedOptions);
        });

        return view;
    }
}