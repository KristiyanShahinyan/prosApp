package digital.paynetics.phos.screens

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.shape.ShapeAppearanceModel
import digital.paynetics.phos.PhosApplication
import digital.paynetics.phos.R
import digital.paynetics.phos.classes.lang.PhosLanguage
import digital.paynetics.phos.classes.lang.PhosString
import digital.paynetics.phos.classes.lang.PhosStringProvider
import digital.paynetics.phos.databinding.FragmentSettingsLanguageBinding
import javax.inject.Inject

class SettingsLanguageFragment : Fragment() {

    private var _binding: FragmentSettingsLanguageBinding? = null

    private val binding get() = _binding!!

    @Inject
    lateinit var stringProvider: PhosStringProvider

    override fun onAttach(context: Context) {
        PhosApplication.getAppComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initVisibility()
        initTitles()
        initOnClickListeners()
        initLanguage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initVisibility() {
    }

    private fun initTitles() {
        binding.header.title.text = stringProvider.getString(PhosString.switch_language)
        binding.save.text = stringProvider.getString(PhosString.save_changes)
    }

    private fun initOnClickListeners() {
        binding.header.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.save.setOnClickListener { saveLanguageChange() }
    }

    private fun initLanguage() {
        for (language in PhosLanguage.values()) {
            if (isLanguageEnabled(language)) {
                binding.languages.addView(createChip(language))
            }
        }
    }

    private fun isLanguageEnabled(language: PhosLanguage) =
        resources.getBoolean(language.enabledBoolRes)

    private fun createChip(language: PhosLanguage): Chip {
        val chip = Chip(requireActivity())
        chip.text = stringProvider.getString(language.phosString)
        chip.tag = language
        chip.isChecked = language == stringProvider.readLanguagePreference()
        return chip
    }

    private fun saveLanguageChange() {
        val language = binding.languages
            .findViewById<Chip>(binding.languages.checkedChipId)
            ?.tag as PhosLanguage
        stringProvider.activateLanguage(language)
        initTitles()
    }
}