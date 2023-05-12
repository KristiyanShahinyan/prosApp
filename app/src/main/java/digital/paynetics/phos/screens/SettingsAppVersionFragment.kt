package digital.paynetics.phos.screens

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import digital.paynetics.phos.PhosApplication
import digital.paynetics.phos.classes.helpers.IdProvider
import digital.paynetics.phos.classes.lang.PhosString
import digital.paynetics.phos.classes.lang.PhosStringProvider
import digital.paynetics.phos.databinding.FragmentSettingsAppVersionBinding
import javax.inject.Inject

class SettingsAppVersionFragment : Fragment() {

    private var _binding: FragmentSettingsAppVersionBinding? = null

    private val binding get() = _binding!!

    @Inject
    lateinit var stringProvider: PhosStringProvider

    @Inject
    lateinit var idProvider: IdProvider

    override fun onAttach(context: Context) {
        PhosApplication.getAppComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsAppVersionBinding.inflate(inflater, container, false)
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
    }

    @SuppressLint("SetTextI18n")
    private fun initTitles() {
        binding.header.title.text = stringProvider.getString(PhosString.app_version)
        binding.versionRow.title.text = getVersionName(requireActivity(), idProvider)
        binding.versionRow.hint.text = getVersionCode(stringProvider, idProvider)
    }

    private fun initVisibility() {}
}