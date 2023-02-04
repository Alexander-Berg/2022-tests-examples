SELECT (local_rank_0_0 = 0 AND local_rank_navi_0_0 > 0 and disp_class = 4 and disp_class_navi = 5 AND priority = 6.125 AND priority_navi = 5.25) FROM {poi_rank_tmp} WHERE ft_id = 1 AND experiment_id IS NULL
UNION ALL
SELECT (local_rank_0_0 > 0 AND local_rank_navi_0_0 = 0 and disp_class = 5 and disp_class_navi = 4) FROM {poi_rank_tmp} WHERE ft_id = 2 AND experiment_id IS NULL
UNION ALL
SELECT (local_rank_0_0 = 0 AND local_rank_navi_0_0 = 0 and disp_class = 4 and disp_class_navi = 3 AND priority = 6.25 and priority_navi = 7.125) FROM {poi_rank_tmp} WHERE ft_id = 1 AND experiment_id = 'exp1'
UNION ALL
SELECT (local_rank_0_0 > 0 AND local_rank_navi_0_0 > 0 and disp_class = 5 and disp_class_navi = 4) FROM {poi_rank_tmp} WHERE ft_id = 2 AND experiment_id = 'exp1'
