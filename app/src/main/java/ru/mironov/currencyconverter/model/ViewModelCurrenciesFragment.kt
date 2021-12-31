package ru.mironov.currencyconverter.model

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.mironov.currencyconverter.repository.Repository
import ru.mironov.currencyconverter.retrofit.JsonRates
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import kotlin.collections.ArrayList

class ViewModelCurrenciesFragment @Inject constructor(val context: Context) : ViewModel() {

    @Inject
    lateinit var repository:Repository

    var mutableStatus = MutableLiveData<Status>()

    private var ratesObject: JsonRates? = null

    var responseCurrency: String? = null

    val arrayRates = ArrayList<CurrencyRate>()

    fun getCurrencyRate(name: String) {
        mutableStatus.postValue(Status.LOADING)
        repository.getRatesFromNetwork(name)
            ?.enqueue(object : Callback<JsonRates?> {
                override fun onResponse(
                    call: Call<JsonRates?>,
                    response: Response<JsonRates?>
                ) {
                    if (response.body() != null) {
                        viewModelScope.launch(Dispatchers.Default) {
                            ratesObject = response.body()

                            if (arrayRates.isEmpty()) {
                                val arrayNames= ArrayList<String>()

                                responseCurrency = ratesObject?.getBaseCurrency().toString()

                                arrayNames.add(responseCurrency!!)

                                //First currency to convert from
                                arrayRates.add(
                                    CurrencyRate(
                                        ratesObject?.getBaseCurrency().toString(),
                                        1.0
                                    )
                                )
                                //Set currencies to convert to
                                ratesObject?.getRates()?.forEach { cur ->
                                    if(cur.key!=ratesObject?.getBaseCurrency().toString()){
                                    arrayRates.add(CurrencyRate(cur.key, cur.value))
                                    arrayNames.add(cur.key)}
                                }

                                repository.saveCurrenciesNames(arrayNames)
                            } else {
                                synchronized(arrayRates) {
                                    arrayRates.forEach { it ->
                                        val rate = ratesObject?.getRates()?.get(it.name)
                                        if (rate != null) {
                                            it.rate = rate
                                        }
                                    }
                                }
                            }


                            mutableStatus.postValue(Status.DATA)
                        }
                    } else {
                        mutableStatus.postValue(Status.ERROR)
                    }
                }

                override fun onFailure(call: Call<JsonRates?>, t: Throwable) {
                    mutableStatus.postValue(Status.ERROR)
                }
            })
    }

    fun swap(pos: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            synchronized(arrayRates) {
                val convRate = arrayRates[pos].rate

                responseCurrency = arrayRates[pos].name

                val tempCur = arrayRates[pos]
                arrayRates.removeAt(pos)
                arrayRates.add(0, tempCur)

                arrayRates.forEach {
                    it.rate = it.rate / convRate
                }
            }
        }
    }
}