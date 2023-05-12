package digital.paynetics.phos.screens

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import digital.paynetics.phos.PhosApplication
import digital.paynetics.phos.classes.lang.PhosString
import digital.paynetics.phos.classes.lang.PhosStringProvider
import digital.paynetics.phos.classes.prefs.PrefState
import digital.paynetics.phos.classes.prefs.RefInfoPreference
import digital.paynetics.phos.classes.prefs.TipPreference
import digital.paynetics.phos.classes.prefs.TogglePreference
import digital.paynetics.phos.databinding.FragmentSettingsScreensBinding
import javax.inject.Inject

class SettingsScreensFragment : Fragment() {

    private var _binding: FragmentSettingsScreensBinding? = null

    private val binding get() = _binding!!

    @Inject
    lateinit var stringProvider: PhosStringProvider

    @Inject
    lateinit var tipPreference: TipPreference

    @Inject
    lateinit var refInfoPreference: RefInfoPreference

    override fun onAttach(context: Context) {
        PhosApplication.getAppComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsScreensBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initVisibility()
        initTitles()
        initOnClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initOnClickListeners() {
        binding.header.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.tipRow.toggle.setOnClickListener {
            setTogglePreference(tipPreference, it as ToggleButton)
        }
        binding.refInfoRow.toggle.setOnClickListener {
            setTogglePreference(refInfoPreference, it as ToggleButton)
        }
    }

    private fun initTitles() {
        binding.header.title.text = stringProvider.getString(PhosString.screen_preferences)
        binding.tipRow.title.text = stringProvider.getString(PhosString.tip)
        binding.refInfoRow.title.text = stringProvider.getString(PhosString.order_reference)

        binding.tipRow.toggle.isChecked = isTipEnabled()
        binding.refInfoRow.toggle.isChecked = isRefInfoEnabled()
    }

    private fun initVisibility() {
        binding.tipRow.root.visibility = if (isTipVisible()) View.VISIBLE else View.GONE
        binding.tipRow.leadingIcon.visibility = View.GONE
        binding.tipRow.leadingIconBackground.visibility = View.GONE
        binding.tipRow.hint.visibility = View.INVISIBLE

        binding.separator.visibility =
            if (isTipVisible() && isRefInfoVisible()) View.VISIBLE else View.GONE

        binding.refInfoRow.root.visibility = if (isRefInfoVisible()) View.VISIBLE else View.GONE
        binding.refInfoRow.leadingIcon.visibility = View.GONE
        binding.refInfoRow.leadingIconBackground.visibility = View.GONE
        binding.refInfoRow.hint.visibility = View.INVISIBLE
    }

    private fun isTipVisible() = tipPreference.isVisible()

    private fun isTipEnabled() = tipPreference.isEnabled()

    private fun isRefInfoVisible() = refInfoPreference.isVisible()

    private fun isRefInfoEnabled() = refInfoPreference.isEnabled()

    private fun setTogglePreference(
        togglePreference: TogglePreference,
        toggleButton: ToggleButton
    ) {
        togglePreference.setDevicePref(if (toggleButton.isChecked) PrefState.ON else PrefState.OFF)
    }
}