categories = [['yt', 1],
['pu', 0],
['ur', 1],
['ph', 1],
['ua', 1],
['kzu', 1],
['kzp', 0],
['usu', 0],
['usp', 0],
['byu', 1],
['byp', 1],
['eu_ur', 0],
['eu_yt', 1],
['sw_ur', 1],
['sw_yt', 1],
['tru', 0],
['trp', 0],
['sw_ytph', 1],
['ytph', 1],
['yt_kzp', 0],
['yt_kzu', 1],
['sw_ph', 0],
['endbuyer_ph', 0],
['endbuyer_ur', 0],
['ur_autoru', 0],
['ph_autoru', 0],
['endbuyer_yt', 0],
['by_ytph', 0],
['am_jp', 1],
['am_np', 1],
['hk_ur', 1],
['hk_yt', 1],
['il_ur', 1],
['az_ur', 1],
['hk_ytph', 1],
['ro_ur', 1],
['fr_ur', 1],
['ur_ytkz', 0],
['gb_ur', 1],
['us_yt', 0],
['us_ytph', 0]]

res = []
for cat in categories:
    res.append(cat[0]+'_0')
    if cat[1] == 1:
        res.append(cat[0]+'_1')
print '\n'.join(res)


