package com.tanercuhadar.yemekkitabi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.tanercuhadar.yemekkitabi.databinding.RecyclerRowBinding
import com.tanercuhadar.yemekkitabi.model.Tarif
import com.tanercuhadar.yemekkitabi.view.ListeFragmentDirections

class TarifAdapter(val tarifListesi : List<Tarif>) : RecyclerView.Adapter<TarifAdapter.TarifHoolder> (){
class TarifHoolder(val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarifHoolder {

      val recyclerRowBinding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return TarifHoolder(recyclerRowBinding)
    }

    override fun getItemCount(): Int {
    return tarifListesi.size
    }

    override fun onBindViewHolder(holder: TarifHoolder, position: Int) {
        holder.binding.recyclerViweTextView.text=tarifListesi[position].isim
        holder.itemView.setOnClickListener{
            val action = ListeFragmentDirections.actionListeFragmentToTarifFragment(bilgi = "eski", id = tarifListesi[position].id)
            Navigation.findNavController(it).navigate(action)
        }
    }

}