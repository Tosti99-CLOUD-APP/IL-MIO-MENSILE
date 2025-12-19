package com.tostiapp.a1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tostiapp.a1.databinding.FragmentTaskListBinding

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private val workViewModel: WorkViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TaskListAdapter(
            onDeleteClicked = { workEntry ->
                workViewModel.delete(workEntry)
            },
            onEditClicked = { workEntry ->
                val bundle = bundleOf("workEntryId" to workEntry.id)
                findNavController().navigate(R.id.action_TaskListFragment_to_EditTaskFragment, bundle)
            }
        )

        binding.taskListRecyclerView.adapter = adapter
        binding.taskListRecyclerView.layoutManager = LinearLayoutManager(context)

        workViewModel.allWorkEntries.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}