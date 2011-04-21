#include <stdio.h>

/*
  File name: utils_C.c
  For dynamical load compile by gcc.
  SHELL> gcc -c utils_C.c ; gcc -shared -o utils_C.so utils_C.o
*/

void convert_event_table(double *event_list_t, int *event_list_action, int *n,
                         double *t_list, int *num_list, int *m){
  double cur_t; 
  int i;
  int num_list_idx = 0;

  if (*n<=0 || *m<=0)
    return;
  cur_t = event_list_t[0];
  num_list[0] = event_list_action[0];
  for (i=1; i<*n; i++){
    if(event_list_t[i] > cur_t){
      cur_t = event_list_t[i];
      ++num_list_idx;
    }
    event_list_action[i] += event_list_action[i-1];
    if (event_list_action[i] > num_list[num_list_idx])      
      num_list[num_list_idx] = event_list_action[i];
  }
}

