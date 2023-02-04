
# coding: utf-8

# In[1]:


import pandas as pd
pd.set_option('display.max_columns', 500)
pd.set_option('display.width', 1000)
pd.set_option('display.max_colwidth', 1000)
import seaborn as sns
import pickle
import numpy as np
import math


# In[2]:


FACTORS = ["area", "final_rooms_encoded", "series_name_encoded", "building_type_str_encoded", 
               "metro_foot_id_encoded", "metro_foot_time",
               "metro_transport_id_encoded", "metro_transport_time",
               "renovation", "kitchen_area", "year", 
               "ponds_time", "ponds_count", "area_segment",
           'building_min', 'building_max', 'building_mean', 'building_std', 'building_median',
'building_min_total', 'building_max_total', 'building_mean_total', 'building_median_total']


# In[3]:


HOUSES_DB_PATH = '../../price-estimator-rest-api/data/all.houses.with.sell.price.stat.tsv'


# In[4]:


MODEL_PATH = '../../price-estimator-rest-api/model/regions_sell_best_model.dat'


# In[5]:


FRAUD_PATH = 'fraud.data.tsv'


# In[6]:


ACTIVE_VOS_PATH = 'active.data.tsv'
ACTIVE_VOS_R3_OFFERS_PATH = 'active.data.r3.offers.tsv'


# In[7]:


houses_df = pd.read_table(HOUSES_DB_PATH)


# In[8]:


model = pickle.load(open(MODEL_PATH, "rb"))


# In[9]:


# houses_df.head()


# In[10]:


# ls ../../price-estimator-rest-api/model/


# In[11]:


fraud_df = pd.read_table(FRAUD_PATH)


# In[65]:


def clean_msc_piter(df, address_key = 'address'):
    df = df[~df[address_key].str.contains('Россия, Москва')]
    df = df[~df[address_key].str.contains('Россия, Санкт-Петербург')]
    df = df[~df[address_key].str.contains('Россия, Ленинградская область')]
    df = df[~df[address_key].str.contains('Россия, Московская область')]
    return df
    


# In[13]:


print(len(fraud_df))


# In[14]:


fraud_df = clean_msc_piter(fraud_df)


# In[15]:


print(len(fraud_df))


# In[16]:


final_df = pd.merge(fraud_df, houses_df, left_on = 'address', right_on = 'unified_address')


# In[17]:


print(len(fraud_df))


# In[18]:


print(len(final_df))


# In[19]:


def encode_rooms(df):
    df['final_rooms_encoded'] = df.rooms
    df.loc[((df.final_rooms_encoded.isnull()) | (df.final_rooms_encoded == 0)) & (df.studio == True), 'final_rooms_encoded'] = 0
    df.loc[((df.final_rooms_encoded.isnull()) | (df.final_rooms_encoded == 0)) & (df.open_plan == True), 'final_rooms_encoded'] = 0


# In[20]:


def addTotalPricesBasedOnArea(df):
    df['building_min_total'] = df.building_min * df.area
    df['building_max_total'] = df.building_max * df.area
    df['building_mean_total'] = df.building_mean * df.area
    df['building_median_total'] = df.building_median * df.area
    df['building_var'] = df.building_std ** 2


# In[21]:


def add_area_segment(df):
    area_bins = [10,30,50,70,100, 1000]
    df['area_segment'] = pd.cut(df['area'], area_bins, labels=False) + 1


# In[22]:


RENOVATION_TO_CODE = {
    'UNKNOWN': 0,
    'DESIGNER_RENOVATION': 1,
    'NEEDS_RENOVATION': 2,
    'NORMAL': 3,
    'PARTIAL_RENOVATION': 4,
    'PRIME_RENOVATION': 5,
    'RENOVATED': 6,
    'COSMETIC_DONE': 7,
    'COSMETIC_REQUIRED': 8,
    'EURO': 10,
    'GOOD': 11,

    'BEFORE_CLEAN': 12,
    'CLEAN': 13,
    'TURNKEY': 14
}
def encode_renovation(df):
    f = lambda x: RENOVATION_TO_CODE.get(x, 0)
    df['renovation'] = df['renovation'].map(f)


# In[23]:


encode_renovation(final_df)


# In[24]:


final_df.head()


# In[25]:


addTotalPricesBasedOnArea(final_df)


# In[26]:


add_area_segment(final_df)


# In[27]:


encode_rooms(final_df)


# In[28]:


pred = model.predict(final_df[FACTORS])


# In[29]:


round_pred = list(map(lambda price: int((price + 500) / 1000) * 1000, pred))


# In[49]:


def get_df_for_analysis(df, pred_df, df_address_key = 'address'):
    df = df[['offer_id', df_address_key, 'area', 'price', 'renovation']]
    round_pred = list(map(lambda price: int((price + 500) / 1000) * 1000, pred_df))
    df['pred_price'] = round_pred
    df['price_pred_diff'] = (df['pred_price'] - df['price'])/df['price']
    return df


# In[31]:


df_pred = get_df_for_analysis(final_df)


# In[32]:


# list(df_pred)


# In[33]:


# list(final_df)


# In[34]:


df_pred.head()


# In[35]:


len(df_pred)


# In[36]:


len(df_pred[df_pred.price_pred_diff > 0.35])


# In[37]:


df_pred[df_pred.price_pred_diff < 0.35]


# In[38]:


df_pred[df_pred.price_pred_diff < 0.50]


# In[39]:


def encode_renovation_float(renovation_float):
    if (renovation_float is np.NaN) or math.isnan(renovation_float):
        renovation_encoded = 0
    else:
        renovation_encoded = int(renovation_float)
    return renovation_encoded


# In[40]:


def encode_renovation_df_float(df):
    df['renovation'] = df['renovation'].map(encode_renovation_float)


# In[50]:


def get_pred_df(df, houses_df, renovation_is_float = False, df_address_key = 'address'):
    final_df = pd.merge(df, houses_df, left_on = df_address_key, right_on = 'unified_address')
    if renovation_is_float:
        encode_renovation_df_float(final_df)
    else:
        encode_renovation(final_df)
    addTotalPricesBasedOnArea(final_df)
    add_area_segment(final_df)
    encode_rooms(final_df)
    pred = model.predict(final_df[FACTORS])
    df_pred = get_df_for_analysis(final_df, pred, df_address_key = df_address_key)
    return df_pred
    


# In[42]:


# active_vos_df = pd.read_table(ACTIVE_VOS_PATH)
active_vos_df = pd.read_table(ACTIVE_VOS_R3_OFFERS_PATH)


# In[43]:


active_vos_df.sample(20)


# In[44]:


# active_vos_df['address_backup'] = active_vos_df['address']


# In[45]:



def get_unified_address(row):
    result = row.address_backup
    if not 'Россия' in row.address_backup:
        result = 'Россия, ' + row.address_backup
    return result
        
    


# In[46]:


# active_vos_df['address'] = active_vos_df.apply(get_unified_address, )


# In[51]:


df_pred_fraud = get_pred_df(fraud_df, houses_df)


# In[52]:


df_pred_active = get_pred_df(active_vos_df, houses_df, 
                             renovation_is_float = True, df_address_key = 'unified_address')


# In[53]:


df_pred_active.sample(30)


# In[58]:


print(len(df_pred_fraud))
print(len(df_pred_fraud[df_pred.price_pred_diff < 0.45]))


# In[60]:


print(len(df_pred_active))
print(len(df_pred_active[df_pred_active.price_pred_diff < 0.45]))


# In[61]:


df_pred_active[df_pred_active.price_pred_diff > 0.35]


# In[66]:


df_pred_active_cleaned = clean_msc_piter(df_pred_active, address_key='unified_address')


# In[68]:


df_pred_active_cleaned[df_pred_active_cleaned.price_pred_diff > 0.35]


# In[69]:


df_pred_active_cleaned[df_pred_active_cleaned.price_pred_diff > 0.35].to_csv('price_less_more_than_35_percent.tsv', index=False, sep='\t')


# In[70]:


active_vos_all_df = pd.read_table('active.vos.sell.regions.tsv')


# In[71]:


df_pred_active = get_pred_df(active_vos_all_df, houses_df, 
                             renovation_is_float = True, df_address_key = 'unified_address')


# In[72]:


df_pred_active.sample(20)


# In[76]:


print(len(df_pred_active))
print(len(df_pred_active[df_pred_active.price_pred_diff < 0.45]))
print(len(df_pred_active[df_pred_active.price_pred_diff < 0.50]))
print(len(df_pred_active[df_pred_active.price_pred_diff < 0.80]))
print(len(df_pred_active[df_pred_active.price_pred_diff < 0.99]))


# In[78]:


print(len(df_pred_active[df_pred_active.price_pred_diff >= 1]))


# In[80]:


df_pred_active[df_pred_active.price_pred_diff >= 1].to_csv('price_less_more_than_100_percent.tsv', index=False, sep='\t')


# In[81]:


df_pred_active[df_pred_active.price_pred_diff >= 1].head()


# In[ ]:


print(len(df_pred_active[df_pred_active.price_pred_diff >= 1]))

